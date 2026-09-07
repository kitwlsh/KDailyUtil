package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.DailyRecord
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager
import com.kitwlshcom.kdailyutil.data.repository.ReadingTrainingRepository
import com.kitwlshcom.kdailyutil.data.repository.RemotePassage
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

    companion object {
        private const val TAG = "ReadingTrainingVM"

        /**
         * 지문 동기화 최소 간격. 탭을 여닫을 때마다 통신하면 데이터만 쓰고 얻는 것이 없다
         * (로봇은 **하루 1편**만 넣는다). 자매앱 목록이 쓰는 6시간과 같은 값으로 맞췄다.
         *
         * 프로세스가 사는 동안만 유지되는 값이다 — 앱을 완전히 껐다 켜면 한 번 더 받는다.
         * 영속 저장까지 할 만한 무게가 아니고, 그렇게 두면 «받아 봐도 소용없는 상태»를 만들 수 있다.
         */
        private const val SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private var lastSyncAtMs = 0L
    }

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

    // ── 오늘의 지문 · 새 지문 (2026-09-07) ──────────────────────
    //
    // 지문이 앱에 하드코딩된 19편뿐이라 매일 하면 19일에 한 바퀴가 돌았다.
    // 로봇이 하루 1편을 넣고, 앱은 그것을 받아 «오늘의 지문»으로 한 편만 정해 준다.
    // 규칙(오늘의 세트·새것 배정·상한·복귀 사면)은 전부 DailyRecord에 있다.

    private val _remotePassages = MutableStateFlow<List<RemotePassage>>(emptyList())

    /** 오늘의 지문 1편. 원격 지문이 하나도 없으면 null → 화면은 내장 지문으로 떨어진다(§8). */
    private val _todayPassage = MutableStateFlow<RemotePassage?>(null)
    val todayPassage: StateFlow<RemotePassage?> = _todayPassage.asStateFlow()

    /** 최근 7일에 도착한 지문(최신순, 최대 [DailyRecord.NEW_LIST_MAX]편). 나머지는 «지난 지문»으로 내린다. */
    private val _newPassages = MutableStateFlow<List<RemotePassage>>(emptyList())
    val newPassages: StateFlow<List<RemotePassage>> = _newPassages.asStateFlow()

    /** 「새 지문 N편」을 어떻게 말할지(상한·복귀 사면 포함). */
    private val _newPassageNotice = MutableStateFlow(DailyRecord.NewItemNotice(unit = "편"))
    val newPassageNotice: StateFlow<DailyRecord.NewItemNotice> = _newPassageNotice.asStateFlow()

    init { refreshPassages(); refreshWpmHistory(); loadRemotePassages(); syncRemotePassages() }

    /** 기기에 있는 것만 먼저 그린다 — 통신을 기다리는 동안 화면이 비어 있으면 안 된다. */
    private fun loadRemotePassages() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.loadRemotePassages() }
            val hidden = repo.hiddenRemoteIdsFlow.first()
            _remotePassages.value = list.filter { it.id.toString() !in hidden }
            recomputePassageState()
        }
    }

    /**
     * 로봇이 올린 지문을 받아 온다. 실패해도 조용히 지나간다 —
     * 못 받으면 저장소가 기존 캐시를 그대로 두고, 화면은 이미 그려져 있다.
     */
    fun syncRemotePassages(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncAtMs < SYNC_INTERVAL_MS) return
        lastSyncAtMs = now
        viewModelScope.launch {
            try {
                repo.syncRemotePassages()
            } catch (e: Exception) {
                Log.e(TAG, "지문 동기화 실패(캐시로 계속): ${e.message}")
            }
            loadRemotePassages()
        }
    }

    private suspend fun recomputePassageState() {
        val pool = _remotePassages.value
        val today = java.time.LocalDate.now()
        val seen = repo.seenPassageCountFlow.first()

        val notice = DailyRecord.newPassageNotice(
            today = today,
            lastTrained = parseCompactDate(repo.lastTrainedDateFlow.first()),
            createdDates = pool.mapNotNull { it.createdAt },
            total = pool.size,
            seenCount = seen
        )

        // 기준점을 지금으로 옮겨야 하는 두 경우 — 퀴즈와 같은 규칙이다.
        //   · 첫 실행(기준값 0) — 놔두면 «지문 수십 편이 새로 왔다»가 된다
        //   · 복귀 사면 — 기준을 리셋해야 「밀린 것」이 다음 날에도 되살아나지 않는다
        if (seen == 0 || notice.amnesty) repo.updateSeenPassageCount(pool.size)

        _newPassageNotice.value = notice
        _newPassages.value = pool
            .filter { d -> d.createdAt?.let { !it.isAfter(today) && today.toEpochDay() - it.toEpochDay() < DailyRecord.FRESH_WINDOW_DAYS } == true }
            .sortedByDescending { it.createdAt }
            .take(DailyRecord.NEW_LIST_MAX)

        _todayPassage.value = pickTodayPassage(pool, today)
    }

    /**
     * 오늘의 지문 = **날짜로 정해지는 1편**. 같은 날은 몇 번 열어도 같고, 자정을 넘기면 바뀐다.
     *
     * 새로 들어온 것이 있으면 그쪽에서 뽑는다([DailyRecord.pickDailyIndices]의 `freshFrom`) —
     * 로봇이 매일 넣는데 사용자가 그걸 영영 못 보는 일이 없게 하는 배려다.
     */
    private fun pickTodayPassage(pool: List<RemotePassage>, today: java.time.LocalDate): RemotePassage? {
        if (pool.isEmpty()) return null
        val ordered = pool.sortedBy { it.createdAt ?: java.time.LocalDate.MIN }
        val freshFrom = ordered.indexOfFirst { p ->
            p.createdAt?.let { today.toEpochDay() - it.toEpochDay() < DailyRecord.FRESH_WINDOW_DAYS } == true
        }
        val index = DailyRecord.pickDailyIndices(
            date = today,
            total = ordered.size,
            count = 1,
            freshFrom = if (freshFrom > 0) freshFrom else 0
        ).firstOrNull() ?: return null
        return ordered.getOrNull(index)
    }

    /** 사용자가 새 지문을 확인했다 → 기준점을 지금으로 옮긴다(퀴즈의 markNewQuizzesSeen과 같은 문법). */
    fun markPassagesSeen() {
        viewModelScope.launch {
            repo.updateSeenPassageCount(_remotePassages.value.size)
            recomputePassageState()
        }
    }

    /** 로봇 지문을 «내 것으로» 보관함에 복사한다. 원본은 그대로 둔다(사용자 소유가 되는 사본). */
    fun copyRemoteToLibrary(passage: RemotePassage) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.addPassage(passage.text, null, System.currentTimeMillis()) }
            refreshPassages()
        }
    }

    /** 목록에서 치운다. 🔴 삭제가 아니다 — 원본은 남고, 사용자에게만 안 보인다. */
    fun hideRemotePassage(passage: RemotePassage) {
        viewModelScope.launch {
            repo.hideRemotePassage(passage.id)
            loadRemotePassages()
        }
    }

    /** 훈련 기록이 쓰는 yyyyMMdd. 깨져 있으면 null(사면 판정을 하지 않는다). */
    private fun parseCompactDate(raw: String?): java.time.LocalDate? = try {
        if (raw.isNullOrBlank()) null
        else java.time.LocalDate.parse(raw, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
    } catch (e: Exception) {
        null
    }


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
