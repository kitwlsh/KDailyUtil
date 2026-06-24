package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.ReadingTrainingRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 이해도 확인용 4지선다 문제 */
data class ComprehensionQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int
)

class ReadingTrainingViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "ReadingTrainingVM" }

    private val repo = ReadingTrainingRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val bestWpm: StateFlow<Int> = repo.bestWpmFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val streak: StateFlow<Int> = repo.streakFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalSessions: StateFlow<Int> = repo.totalSessionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val bestComprehension: StateFlow<Int> = repo.bestComprehensionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _isExtractingText = MutableStateFlow(false)
    val isExtractingText: StateFlow<Boolean> = _isExtractingText.asStateFlow()

    /** 책 페이지 사진에서 본문 텍스트를 OCR 추출 (@param onResult (text, error)) */
    fun extractTextFromImage(bitmap: Bitmap, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            _isExtractingText.value = true
            try {
                val key = settingsRepository.geminiApiKeyFlow.first()
                if (key.isNullOrBlank()) {
                    onResult(null, "설정 > AI·키 에서 Gemini API Key를 먼저 등록해 주세요.")
                    return@launch
                }
                val text = GeminiManager(key).extractTextFromImage(bitmap)
                if (text.isBlank()) onResult(null, "이미지에서 글자를 찾지 못했습니다. 더 선명하게 다시 시도해 주세요.")
                else onResult(text, null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ extractTextFromImage 실패: ${e.message}")
                onResult(null, "텍스트 추출 오류: ${e.message}")
            } finally {
                _isExtractingText.value = false
            }
        }
    }

    /** 한 세션 완료 기록 (wpm=0이면 워밍업 등 속도 무관 세션) */
    fun recordSession(wpm: Int) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val today = sdf.format(cal.time)
            cal.add(Calendar.DATE, -1)
            val yesterday = sdf.format(cal.time)
            repo.recordSession(wpm, today, yesterday)
        }
    }

    /** 이해도 점수(0~100) 최고치 기록 */
    fun recordComprehension(scorePercent: Int) {
        viewModelScope.launch { repo.recordComprehension(scorePercent) }
    }

    /**
     * 읽은 지문으로 Gemini 이해도 퀴즈를 생성한다.
     * @param onResult (문제목록, 에러메시지) — 성공 시 list!=null, 실패 시 error!=null
     */
    fun generateComprehension(passage: String, onResult: (List<ComprehensionQuestion>?, String?) -> Unit) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            try {
                val key = settingsRepository.geminiApiKeyFlow.first()
                if (key.isNullOrBlank()) {
                    onResult(null, "설정 > AI·키 에서 Gemini API Key를 먼저 등록해 주세요.")
                    return@launch
                }
                val raw = GeminiManager(key).generateComprehensionQuiz(passage)
                val start = raw.indexOf('[')
                val end = raw.lastIndexOf(']')
                if (start < 0 || end <= start) {
                    onResult(null, "이해도 문제를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.")
                    return@launch
                }
                val arr = JSONArray(raw.substring(start, end + 1))
                val list = (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val optsArr = o.optJSONArray("options") ?: return@mapNotNull null
                    val opts = (0 until optsArr.length()).map { optsArr.optString(it) }
                    if (opts.size < 2) return@mapNotNull null
                    ComprehensionQuestion(
                        question = o.optString("question"),
                        options = opts,
                        answerIndex = o.optInt("answerIndex", 0).coerceIn(0, opts.lastIndex)
                    )
                }
                if (list.isEmpty()) onResult(null, "이해도 문제를 생성하지 못했습니다.")
                else onResult(list, null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ generateComprehension 실패: ${e.message}")
                onResult(null, "분석 오류: ${e.message}")
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }
}
