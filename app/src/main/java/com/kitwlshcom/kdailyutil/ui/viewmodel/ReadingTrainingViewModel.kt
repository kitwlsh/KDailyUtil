package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.PassageKey
import com.kitwlshcom.kdailyutil.data.DailyRecord
import com.kitwlshcom.kdailyutil.data.ReadingTrainingModule
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

    /**
     * 사용자가 정한 **기본 훈련**. 정한 적이 없으면 [ReadingTrainingModule.DEFAULT](리듬 페이서).
     * 지문 카드의 시작 버튼은 **항상 이 값**으로 시작한다.
     */
    val defaultModule: StateFlow<ReadingTrainingModule> =
        repo.defaultModuleFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingTrainingModule.DEFAULT
        )

    /**
     * 🔴 **「⭐ 기본으로 정하기」에서만 부른다.** 훈련을 시작하는 것만으로 이걸 부르면
     * 「다음 훈련」 한 번에 기본이 갈아치워지던 2026-09-14 이전 동작으로 되돌아간다.
     */
    fun setDefaultModule(module: ReadingTrainingModule) {
        viewModelScope.launch { repo.setDefaultModule(module) }
    }

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

    /**
     * 받아 둔 원격 지문 **전체**(치운 것 제외). 「지문 고르기」 목록이 쓴다.
     *
     * 🔴 여기에 «안 읽음 배지»나 «진행률»을 붙이지 말 것 — 그 순간 고르는 목록이 아니라
     * 밀린 것을 세는 목록이 된다(doc/FEATURE_DAILY_PASSAGES.md §6-6에서 하지 말 것으로 정한 항목).
     */
    val allRemotePassages: StateFlow<List<RemotePassage>> = _remotePassages.asStateFlow()

    /** 오늘의 지문 1편. 원격 지문이 하나도 없으면 null → 화면은 내장 지문으로 떨어진다(§8). */
    private val _todayPassage = MutableStateFlow<RemotePassage?>(null)
    val todayPassage: StateFlow<RemotePassage?> = _todayPassage.asStateFlow()

    /** 최근 7일에 도착한 지문(최신순, 최대 [DailyRecord.NEW_LIST_MAX]편). 나머지는 «지난 지문»으로 내린다. */
    private val _newPassages = MutableStateFlow<List<RemotePassage>>(emptyList())
    val newPassages: StateFlow<List<RemotePassage>> = _newPassages.asStateFlow()

    /** 「새 지문 N편」을 어떻게 말할지(상한·복귀 사면 포함). */
    private val _newPassageNotice = MutableStateFlow(DailyRecord.NewItemNotice(unit = "편"))
    val newPassageNotice: StateFlow<DailyRecord.NewItemNotice> = _newPassageNotice.asStateFlow()

    /** 지문을 받아오는 중인가. 첫 설치에는 «받아오는 중»을 말해 줘야 빈 화면이 되지 않는다. */
    private val _passageSyncing = MutableStateFlow(false)
    val passageSyncing: StateFlow<Boolean> = _passageSyncing.asStateFlow()

    /** 마지막 받아오기가 실패했는가(오프라인 포함). */
    private val _passageSyncFailed = MutableStateFlow(false)
    val passageSyncFailed: StateFlow<Boolean> = _passageSyncFailed.asStateFlow()

    /** 사용자가 「숨기기」로 감춘 지문 수. 0보다 크면 다시 꺼내는 길을 화면에 내준다. */
    private val _hiddenPassageCount = MutableStateFlow(0)
    val hiddenPassageCount: StateFlow<Int> = _hiddenPassageCount.asStateFlow()

    /** 오늘의 지문이 «지난 지문을 다시 꺼낸 것»인가. 화면이 그 사실을 말해 주게 한다. */
    private val _todayIsRevisit = MutableStateFlow(false)
    val todayIsRevisit: StateFlow<Boolean> = _todayIsRevisit.asStateFlow()

    init { refreshPassages(); refreshWpmHistory(); loadRemotePassages(); syncRemotePassages() }

    /** 기기에 있는 것만 먼저 그린다 — 통신을 기다리는 동안 화면이 비어 있으면 안 된다. */
    private fun loadRemotePassages() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.loadRemotePassages() }
            val hidden = repo.hiddenRemoteIdsFlow.first()
            _remotePassages.value = list.filter { it.id.toString() !in hidden }
            _hiddenPassageCount.value = list.count { it.id.toString() in hidden }
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
            _passageSyncing.value = true
            // 🔴 **«받아오는 중» 깃발은 목록이 실제로 채워진 뒤에 내린다**(2026-09-16).
            //    예전에는 내려받기가 끝나자마자 내렸는데, 목록을 읽어 오는 것은 그다음
            //    IO였다. 그 짧은 틈에 화면은 «지문 0편 + 받아오는 중 아님»을 보고
            //    **첫 설치에서 성공한 동기화인데도 「지금은 받아오지 못했어요」 + [다시 시도]**를
            //    번쩍였다. 09-14에 만든 그 안내가 스스로 거짓말을 하고 있었다.
            try {
                try {
                    repo.syncRemotePassages()
                    _passageSyncFailed.value = false
                } catch (e: Exception) {
                    Log.e(TAG, "지문 동기화 실패(캐시로 계속): ${e.message}")
                    _passageSyncFailed.value = true
                }
                loadRemotePassages()
            } finally {
                _passageSyncing.value = false
            }
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
        // 🔴 7일에 하루는 신규 배려를 끄고 **전체에서** 뽑는다(2026-09-14).
        //    그러지 않으면 지문은 일주일만 살아 있고 8일째부터 영영 오늘의 지문이 되지 않는다.
        //    ⚠️ «지난 지문이 실제로 존재할 때»만 켠다 — 전부 새것이면 다시 꺼낼 것이 없다.
        val hasOlder = freshFrom > 0
        val revisitDay = hasOlder && DailyRecord.isPassageRevisitDay(today)
        val index = DailyRecord.pickDailyIndices(
            date = today,
            total = ordered.size,
            count = 1,
            freshFrom = if (hasOlder && !revisitDay) freshFrom else 0
        ).firstOrNull()
        if (index == null) {
            _todayIsRevisit.value = false
            return null
        }
        // 🔴 **배지는 «뽑힌 것»을 보고 정한다**(2026-09-16에 고쳤다).
        //    예전에는 «오늘이 지난 지문의 날인가»만 보고 미리 정했는데, 그날의 뽑기는
        //    신규 배려를 끈 «전체»에서 하므로 **이번 주에 온 지문이 뽑힐 수도 있다**.
        //    그러면 새 지문에 「🔁 지난 지문」이 붙어 사용자에게 거짓말이 된다.
        //    `ordered`는 날짜 오름차순이라 **freshFrom보다 앞 = 지난 지문**이다.
        _todayIsRevisit.value = DailyRecord.isRevisitPick(index, freshFrom)
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

    /** 숨긴 지문을 전부 다시 꺼낸다 — 「숨기기」에 돌아올 길이 없으면 그것은 삭제다. */
    fun restoreHiddenPassages() {
        viewModelScope.launch {
            repo.restoreHiddenRemotePassages()
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
    /**
     * 한 세션 완료 기록.
     *
     * 🔴 **재독은 속도 기록에 넣지 않는다**(2026-09-14 · 사용자 지적).
     * 같은 지문을 다시 읽으면 내용을 아니까 더 빠른 속도로도 «따라갈» 수 있다.
     * 그것을 실력으로 세면 ① WPM 추이가 거짓말이 되고 ② 「추천 목표」(최근 5회 평균 × 1.08)까지
     * 함께 부풀어 **다음 목표가 더 헛돌게** 된다.
     *
     * ⚠️ **재독도 «훈련은 한 것»이다** — 출석·연속·누적 횟수는 그대로 인정한다.
     * 빼는 것은 **속도 숫자뿐**이다. 그러지 않으면 «복습하면 손해»가 되어 재독을 벌주게 된다.
     *
     * 🔴 **`wpmHistory`의 형식은 건드리지 않았다**(숫자 목록 그대로).
     * 재독을 «넣되 표시만» 하려면 형식을 바꿔야 하고 기존 사용자 데이터 이전이 따라온다.
     * 「넣지 않는다」로 정하면 형식도 그대로고 추천 목표 오염도 같이 풀린다.
     *
     * @param passage 이번에 읽은 지문. 비어 있으면(워밍업·안구 추적) 재독 판정을 하지 않는다.
     */
    fun recordSession(wpm: Int, passage: String = "") {
        // 🔴 **먼저 «모름»으로 되돌린다.** 이 줄은 결과 화면이 그려지기 전에,
        //    코루틴 밖에서 동기적으로 실행돼야 직전 판정이 새어 나가지 않는다.
        _lastSessionWasRepeat.value = null
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val today = sdf.format(cal.time)
            cal.add(Calendar.DATE, -1)
            val yesterday = sdf.format(cal.time)

            val firstRead = repo.markReadAndCheckFirst(PassageKey.of(passage))
            _lastSessionWasRepeat.value = wpm > 0 && passage.isNotBlank() && !firstRead

            repo.recordSession(wpm, today, yesterday, countSpeed = firstRead)
            if (wpm > 0 && firstRead) {
                withContext(Dispatchers.IO) { repo.addWpmHistory(wpm) }
                refreshWpmHistory()
            }
        }
    }

    /**
     * 방금 끝낸 세션이 **재독이었는가**. 결과 화면이 «왜 기록이 안 올라갔는지» 말해 주기 위해 필요하다.
     * 🔴 말해 주지 않으면 사용자는 «앱이 고장났나»로 읽는다.
     */
    // 🔴 **null = «아직 모른다»**(2026-09-16에 Boolean에서 바꿨다).
    //    재독 판정은 DataStore를 한 번 다녀와야 나오는데, 결과 화면은 그 전에 그려진다.
    //    Boolean이던 시절에는 그 한 틈에 **직전 세션의 판정**이 먼저 보였다 —
    //    새 지문을 읽고도 「기록에는 넣지 않음」이 떴다(그 반대도 됐다).
    //    모르는 동안에는 **아무 말도 하지 않는 것**이 맞다.
    private val _lastSessionWasRepeat = MutableStateFlow<Boolean?>(null)
    val lastSessionWasRepeat: StateFlow<Boolean?> = _lastSessionWasRepeat.asStateFlow()

    /** 지금까지 «처음» 읽은 지문 편수 — 통계 화면의 설명에 쓴다. */
    val readPassageCount: StateFlow<Int> =
        repo.readPassageCountFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
