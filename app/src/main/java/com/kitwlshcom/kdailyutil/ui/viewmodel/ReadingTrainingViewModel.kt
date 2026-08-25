package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.ReadingTrainingRepository
import com.kitwlshcom.kdailyutil.data.repository.SavedPassage
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val trainedDates: StateFlow<Set<String>> = repo.trainedDatesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _wpmHistory = MutableStateFlow<List<Int>>(emptyList())
    val wpmHistory: StateFlow<List<Int>> = _wpmHistory.asStateFlow()

    /**
     * 난이도 자동 추천: 최근 기록(최대 5회) 평균을 약 8% 상향한 '다음 목표 속도'(WPM).
     * 기록이 없으면 일반 성인 평균에 가까운 300으로 시작. 드릴 초기 속도·통계 화면에 사용.
     */
    val recommendedWpm: StateFlow<Int> = _wpmHistory
        .map { computeRecommendedWpm(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300)

    private fun computeRecommendedWpm(history: List<Int>): Int {
        if (history.isEmpty()) return 300
        val recent = history.takeLast(5)
        val target = (recent.average() * 1.08).toInt()
        return (target / 10 * 10).coerceIn(150, 700) // 10단위 반올림 + 슬라이더 범위로 clamp
    }

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _isExtractingText = MutableStateFlow(false)
    val isExtractingText: StateFlow<Boolean> = _isExtractingText.asStateFlow()

    // 지문 보관함
    private val _savedPassages = MutableStateFlow<List<SavedPassage>>(emptyList())
    val savedPassages: StateFlow<List<SavedPassage>> = _savedPassages.asStateFlow()

    init { refreshPassages(); refreshWpmHistory() }

    fun refreshPassages() {
        viewModelScope.launch { _savedPassages.value = withContext(Dispatchers.IO) { repo.loadPassages() } }
    }

    fun refreshWpmHistory() {
        viewModelScope.launch { _wpmHistory.value = withContext(Dispatchers.IO) { repo.loadWpmHistory() } }
    }

    /** 촬영/추출한 페이지를 이미지 썸네일과 함께 보관함에 저장 */
    fun savePassageFromImage(bitmap: android.graphics.Bitmap, text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val path = repo.saveImage(bitmap)
                repo.addPassage(text, path, System.currentTimeMillis())
            }
            refreshPassages()
        }
    }

    /** 붙여넣은 텍스트를 보관함에 저장 */
    fun savePassageText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.addPassage(text, null, System.currentTimeMillis()) }
            refreshPassages()
        }
    }

    fun deletePassage(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deletePassage(id) }
            refreshPassages()
        }
    }

    /** 보관함 지문 제목 변경 */
    fun renamePassage(id: String, newTitle: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.renamePassage(id, newTitle) }
            refreshPassages()
        }
    }

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
                if (text.isBlank()) onResult(null, "글자를 추출하지 못했습니다. 글자가 선명하게 나오도록 다시 촬영하거나, AI 사용량 초과일 수 있으니 잠시 후 다시 시도해 주세요.")
                else onResult(text, null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ extractTextFromImage 실패: ${e.message}")
                onResult(null, "글자를 추출하지 못했습니다.\n" + GeminiManager.aiErrorMessage(e))
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
            if (wpm > 0) {
                withContext(Dispatchers.IO) { repo.addWpmHistory(wpm) }
                refreshWpmHistory()
            }
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
                onResult(null, "이해도 문제를 만들지 못했습니다.\n" + GeminiManager.aiErrorMessage(e))
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }
}
