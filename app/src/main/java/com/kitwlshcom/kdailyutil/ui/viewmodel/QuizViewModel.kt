package com.kitwlshcom.kdailyutil.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import com.kitwlshcom.kdailyutil.data.repository.QuizRepository
import com.kitwlshcom.kdailyutil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.kitwlshcom.kdailyutil.data.remote.GeminiManager

enum class QuizState
{
    IDLE,
    PLAYING,
    ANSWER_CHECKED,
    FINISHED,
    CATEGORY_SELECTION,
    GENERATING,
    CREATOR
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository()
    private val settingsRepository = SettingsRepository(application)

    private val soundPool: android.media.SoundPool
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var finishSoundId: Int = 0

    init {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = android.media.SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        val context = application.applicationContext
        try {
            correctSoundId = soundPool.load(context, com.kitwlshcom.kdailyutil.R.raw.quiz_correct, 1)
            wrongSoundId = soundPool.load(context, com.kitwlshcom.kdailyutil.R.raw.quiz_wrong, 1)
            finishSoundId = soundPool.load(context, com.kitwlshcom.kdailyutil.R.raw.quiz_finish, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }

    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions: StateFlow<List<QuizQuestion>> = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _quizState = MutableStateFlow(QuizState.IDLE)
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(listOf("우리말 겨루기", "트렌드 말하기", "상식 백과", "세계 여행", "AI 자동 생성 (KuizGenius)"))
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _pendingImport = MutableStateFlow<com.kitwlshcom.kdailyutil.data.ImportedQuizPackage?>(null)
    val pendingImport: StateFlow<com.kitwlshcom.kdailyutil.data.ImportedQuizPackage?> = _pendingImport.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()

    // 힌트 관련 상태
    private val _hintLevel = MutableStateFlow(0)
    val hintLevel: StateFlow<Int> = _hintLevel.asStateFlow()

    private val _filteredOptions = MutableStateFlow<List<String>?>(null)
    val filteredOptions: StateFlow<List<String>?> = _filteredOptions.asStateFlow()

    private val _currentHintText = MutableStateFlow<String?>(null)
    val currentHintText: StateFlow<String?> = _currentHintText.asStateFlow()

    private val _isCheckingAnswer = MutableStateFlow(false)

    // ──────────────────────────────────────────────────────────────
    // 🔁 오늘의 퀴즈 · 출석 · 오답 노트 (2026-09-04)
    // ──────────────────────────────────────────────────────────────

    /** 지금 돌고 있는 퀴즈가 무엇인지. 끝났을 때 «출석을 찍을지»가 이걸로 갈린다. */
    enum class Mode { NORMAL, DAILY, REVIEW }

    private val _mode = MutableStateFlow(Mode.NORMAL)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    /** 출석·연속·누적 기록. 화면이 조각을 다시 합칠 필요가 없게 한 덩어리로 준다. */
    val dailyStatus = settingsRepository.dailyStatusFlow.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        com.kitwlshcom.kdailyutil.data.repository.SettingsRepository.DailyStatus()
    )

    /** 마지막으로 본 뒤로 새로 들어온 문제 수. 0이면 배지를 숨긴다. */
    private val _newQuizCount = MutableStateFlow(0)
    val newQuizCount: StateFlow<Int> = _newQuizCount.asStateFlow()

    /** 아직 한 번도 못 맞힌 문제 수(오답 노트 배지). */
    private val _wrongToReviewCount = MutableStateFlow(0)
    val wrongToReviewCount: StateFlow<Int> = _wrongToReviewCount.asStateFlow()

    /** 오늘 세트를 방금 끝냈을 때 한 번만 띄우는 축하 정보. 화면이 소비하면 null로 지운다. */
    private val _dailyCelebration = MutableStateFlow<DailyCelebration?>(null)
    val dailyCelebration: StateFlow<DailyCelebration?> = _dailyCelebration.asStateFlow()

    data class DailyCelebration(
        val streak: Int,
        val correct: Int,
        val total: Int,
        val newBadges: List<com.kitwlshcom.kdailyutil.data.DailyRecord.Badge>
    )

    fun consumeCelebration() { _dailyCelebration.value = null }
    val isCheckingAnswer: StateFlow<Boolean> = _isCheckingAnswer.asStateFlow()

    fun syncRemoteData() {
        viewModelScope.launch {
            repository.syncRemoteQuizzes(getApplication())
            refreshCounters()
        }
    }

    /**
     * 「새 문제 N개」·「복습할 N개」를 다시 센다.
     *
     * 자동 생성 로봇이 매일 문제를 넣고 있는데도 앱이 그 사실을 말해 주지 않으면,
     * 사용자에게 어제와 오늘의 앱은 완전히 똑같다. 열 이유가 없어진다.
     */
    fun refreshCounters() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val total = repository.countAll(context)
                val seen = settingsRepository.dailyStatusFlow.first().seenQuizCount
                // 처음 실행(기준값 0)이면 «500개가 새로 왔다»가 되어 버린다 → 조용히 기준만 잡는다.
                if (seen == 0) {
                    settingsRepository.updateSeenQuizCount(total)
                    _newQuizCount.value = 0
                } else {
                    _newQuizCount.value = (total - seen).coerceAtLeast(0)
                }
                _wrongToReviewCount.value = com.kitwlshcom.kdailyutil.data.QuizStatsManager
                    .getInstance(context).getUnresolvedWrongCount()
            } catch (e: Exception) {
                Log.e("QuizViewModel", "카운터 갱신 실패: ${e.message}")
            }
        }
    }

    /** 사용자가 새 문제를 확인했다 → 기준점을 지금으로 옮긴다. */
    fun markNewQuizzesSeen() {
        viewModelScope.launch {
            try {
                val total = repository.countAll(getApplication<Application>().applicationContext)
                settingsRepository.updateSeenQuizCount(total)
                _newQuizCount.value = 0
            } catch (e: Exception) {
                Log.e("QuizViewModel", "새 문제 기준점 갱신 실패: ${e.message}")
            }
        }
    }

    /**
     * 오늘의 퀴즈를 시작한다 — 날짜로 정해진 다섯 문제.
     *
     * 기존 랜덤 모드와 달리 «끝»이 있다. 끝이 없으면 «언제 해도 되니까 안 하게» 되고,
     * 그래서 내일 다시 올 이유도 생기지 않는다.
     */
    fun startDailyQuiz() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val today = java.time.LocalDate.now()
            val daily = repository.getDailyQuizzes(context, today)

            if (daily.isEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "오늘의 문제를 준비하지 못했어요. 인터넷 연결 후 다시 시도해 주세요.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            _mode.value = Mode.DAILY
            _selectedCategory.value = "오늘의 퀴즈"
            _questions.value = daily
            startLoadedQuestions()
        }
    }

    /** 오답 노트 — 틀린 문제만 다시. 통계는 이미 쌓여 있었는데 볼 곳이 없었다. */
    fun startReviewQuiz() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val wrong = repository.getWrongQuizzes(context, limit = 10)

            if (wrong.isEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "아직 복습할 오답이 없어요. 문제를 풀면 여기에 모입니다.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            _mode.value = Mode.REVIEW
            _selectedCategory.value = "오답 노트"
            _questions.value = wrong
            startLoadedQuestions()
        }
    }

    /** _questions에 문항을 넣은 뒤의 공통 초기화. */
    private fun startLoadedQuestions() {
        // 지난 축하 배너를 지운다. 안 지우면 다음 퀴즈 결과 화면에까지 「오늘 완료」가 따라붙는다.
        _dailyCelebration.value = null
        _currentIndex.value = 0
        _score.value = 0
        _quizState.value = QuizState.PLAYING
        _currentInput.value = ""
        _isCorrect.value = false
        resetHintState()
    }

    fun selectCategory(category: String?)
    {
        _selectedCategory.value = category
        if (category == null)
        {
            loadCategories()
            _quizState.value = QuizState.CATEGORY_SELECTION
        }
        else if (category != "AI 자동 생성 (KuizGenius)")
        {
            startQuiz()
        }
    }

    // 시스템 뒤로가기용: 분야 선택 화면에서 한 단계 더 뒤로(초기 화면)로 이동.
    fun backToIdle()
    {
        _selectedCategory.value = null
        _quizState.value = QuizState.IDLE
    }

    fun startQuiz() {
        _mode.value = Mode.NORMAL
        _dailyCelebration.value = null
        viewModelScope.launch {
            val allQuestions = repository.getQuizzes(getApplication(), _selectedCategory.value)
            
            // 같은 '질문'이 반복 출제되지 않게만 걸러낸다.
            // (예전엔 '정답' 기준으로 걸러서, 정답이 비어있는 문제 - 예: 그림 매칭 퀴즈 - 가
            //  통째로 사라질 수 있었음. 정답 기준 중복 제거는 이미 QuizRepository.getQuizzes(dedupeQuizzes)가 처리함)
            val uniqueQuestions = mutableListOf<QuizQuestion>()
            val seenQuestions = mutableSetOf<String>()
            for (q in allQuestions) {
                val normalizedQuestion = q.question.replace(Regex("\\s+"), "").lowercase()
                if (normalizedQuestion.isNotEmpty() && seenQuestions.add(normalizedQuestion)) {
                    uniqueQuestions.add(q)
                }
            }
            
            // 문항이 하나도 없으면 PLAYING으로 넘어가지 않는다.
            // (넘어가면 문제 화면이 아무것도 못 그려 '빈 화면'처럼 보임 — 데이터 미동기화/파싱 실패 대비 최후 방어선)
            if (uniqueQuestions.isEmpty()) {
                android.widget.Toast.makeText(
                    getApplication(),
                    "표시할 문제가 없어요. 인터넷 연결 후 잠시 뒤 다시 시도해 주세요.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                _quizState.value = QuizState.CATEGORY_SELECTION
                return@launch
            }

            _questions.value = uniqueQuestions.take(10)
            _currentIndex.value = 0
            _score.value = 0
            _quizState.value = QuizState.PLAYING
            _currentInput.value = ""
            _isCorrect.value = false
            resetHintState()
        }
    }

    /**
     * 주제로 AI 퀴즈를 생성한다.
     *
     * @param onError 사용자에게 보여줄 사유(키 미등록·생성 실패 등). 호출부가 Toast/다이얼로그로 노출한다.
     *
     * ⚠️ 예전에는 키가 없으면 **안내 없이 조용히 기존 퀴즈를 시작**했다(`startQuiz()`).
     * 사용자는 'AI 퀴즈 생성'을 눌렀는데 엉뚱한 일반 퀴즈가 시작돼 버그로 보였고,
     * 다른 AI 기능 13곳이 전부 "설정에서 키를 등록해 주세요"를 안내하는 것과도 어긋났다.
     */
    fun generateAiQuiz(topic: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val apiKey = settingsRepository.geminiApiKeyFlow.first()
            if (apiKey.isNullOrBlank()) {
                _quizState.value = QuizState.CATEGORY_SELECTION
                onError("설정 > AI·키 에서 Gemini API Key를 먼저 등록해 주세요.")
                return@launch
            }
            _quizState.value = QuizState.GENERATING

            try {
                val gemini = GeminiManager(apiKey)
                val jsonString = gemini.generateQuizFromText(topic)
                val jsonArray = JSONArray(jsonString)
                val aiQuestions = mutableListOf<QuizQuestion>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val optionsArray = obj.optJSONArray("options")
                    val optionsList = if (optionsArray != null) {
                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                    } else null

                    aiQuestions.add(
                        QuizQuestion(
                            id = 1000 + i,
                            type = QuizType.fromRaw(obj.optString("type"), optionsList != null),
                            category = "AI 자동 생성",
                            subCategory = topic,
                            question = obj.getString("question"),
                            options = optionsList,
                            answer = obj.getString("answer"),
                            explanation = obj.getString("explanation"),
                            semanticHint = obj.optString("semanticHint", null)
                        )
                    )
                }
                
                _questions.value = aiQuestions
                _currentIndex.value = 0
                _score.value = 0
                _quizState.value = QuizState.PLAYING
                _currentInput.value = ""
                _isCorrect.value = false
                resetHintState()
            } catch (e: Exception) {
                Log.e("QuizViewModel", "❌ AI Quiz Generation Failed: ${e.message}", e)
                // 에러 발생 시 초기 화면으로 — 사유는 사용자에게 알린다(조용히 삼키지 않는다)
                _quizState.value = QuizState.CATEGORY_SELECTION
                onError("AI 퀴즈를 만들지 못했습니다.\n" + GeminiManager.aiErrorMessage(e))
            }
        }
    }

    private fun resetHintState() {
        _hintLevel.value = 0
        val currentQ = _questions.value.getOrNull(_currentIndex.value)
        _filteredOptions.value = currentQ?.options
        _currentHintText.value = null
    }

    fun requestHint() {
        val currentQ = _questions.value.getOrNull(_currentIndex.value) ?: return
        
        _hintLevel.value += 1
        val level = _hintLevel.value

        if (currentQ.type == QuizType.MULTIPLE_CHOICE) {
            // 객관식 힌트 로직
            val options = currentQ.options ?: return
            val answer = currentQ.answer
            val wrongOptions = options.filter { it != answer }.shuffled()
            
            if (level == 1) {
                // 오답 1개 제거 (3개 남음)
                if (wrongOptions.isNotEmpty()) {
                    _filteredOptions.value = options.filter { it != wrongOptions[0] }
                }
                _currentHintText.value = currentQ.semanticHint
            } else if (level >= 2) {
                // 오답 2개 제거 (2개 남음 - 50:50)
                if (wrongOptions.size >= 2) {
                    val removed = wrongOptions.take(2)
                    _filteredOptions.value = options.filter { !removed.contains(it) }
                }
                _currentHintText.value = currentQ.semanticHint
            }
        } else {
            // 주관식 힌트 로직
            val answer = currentQ.answer.replace(Regex("\\(.*?\\)"), "").trim()
            when (level) {
                1 -> _currentHintText.value = "글자 수: ${answer.length}글자\n(의미: ${currentQ.semanticHint ?: ""})"
                2 -> {
                    val chosung = answer.map { getChosung(it) }.joinToString("")
                    _currentHintText.value = "초성: $chosung\n(의미: ${currentQ.semanticHint ?: ""})"
                }
                else -> {
                    val chosungList = answer.map { getChosung(it) }
                    val mixed = answer.mapIndexed { index, char ->
                        if (index % 2 == 0) char.toString() else chosungList[index].toString()
                    }.joinToString("")
                    _currentHintText.value = "일부 글자 공개: $mixed\n(의미: ${currentQ.semanticHint ?: ""})"
                }
            }
        }
    }

    private fun getChosung(char: Char): Char {
        val choList = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
        if (char in '가'..'힣') {
            val code = char.code - 0xAC00
            val choIndex = code / (21 * 28)
            return choList[choIndex]
        }
        return char
    }

    fun updateInput(input: String) {
        if (_quizState.value == QuizState.PLAYING) {
            _currentInput.value = input
        }
    }

    fun checkAnswer()
    {
        if (_quizState.value != QuizState.PLAYING) return
        if (_isCheckingAnswer.value) return // 중복 실행 방지

        val currentQuestion = _questions.value[_currentIndex.value]
        
        viewModelScope.launch {
            _isCheckingAnswer.value = true

            // 주관식일 경우 괄호 안의 내용(한자 등)을 제거하고 앞뒤 공백 제거
            val cleanUserAnswer = _currentInput.value.replace(Regex("\\(.*?\\)"), "").trim()
            val cleanCorrectAnswer = currentQuestion.answer.replace(Regex("\\(.*?\\)"), "").trim()

            var correct = false

            if (currentQuestion.type == QuizType.MULTIPLE_CHOICE) {
                // 객관식은 정확히 대소문자 무시 일치 여부 판정
                correct = cleanUserAnswer.equals(cleanCorrectAnswer, ignoreCase = true)
            } else {
                // 주관식 채점 로직
                
                // 1단계: 로컬 정밀 휴리스틱 비교 (공백 완전 무시, 특수문자 및 기호 제거, 대소문자 무시)
                val cleanUserHeuristic = cleanUserAnswer.replace(" ", "").replace(Regex("[^a-zA-Z0-9가-힣]"), "")
                val cleanCorrectHeuristic = cleanCorrectAnswer.replace(" ", "").replace(Regex("[^a-zA-Z0-9가-힣]"), "")
                
                if (cleanUserHeuristic.equals(cleanCorrectHeuristic, ignoreCase = true)) {
                    correct = true
                } else {
                    // 2단계: Gemini AI 채점 (API 키가 존재할 때에만 비동기로 실행)
                    val apiKey = settingsRepository.geminiApiKeyFlow.first()
                    if (!apiKey.isNullOrBlank()) {
                        try {
                            val gemini = GeminiManager(apiKey)
                            correct = gemini.verifySubjectiveAnswer(
                                question = currentQuestion.question,
                                correctAnswer = cleanCorrectAnswer,
                                userAnswer = cleanUserAnswer
                            )
                        } catch (e: Exception) {
                            Log.e("QuizViewModel", "❌ AI Subjective Answer Check Failed: ${e.message}", e)
                        }
                    }
                }
            }

            _isCorrect.value = correct
            
            if (correct)
            {
                _score.value += 1
                if (correctSoundId != 0) soundPool.play(correctSoundId, 1f, 1f, 0, 0, 1f)
            }
            else
            {
                if (wrongSoundId != 0) soundPool.play(wrongSoundId, 1f, 1f, 0, 0, 1f)
            }
            
            _quizState.value = QuizState.ANSWER_CHECKED
            _isCheckingAnswer.value = false

            val context = getApplication<Application>().applicationContext
            try {
                com.kitwlshcom.kdailyutil.data.QuizStatsManager.getInstance(context)
                    .recordQuizResult(currentQuestion.category, currentQuestion.question, correct)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "❌ Failed to record quiz result: ${e.message}")
            }
        }
    }

    fun nextQuestion() {
        if (_currentIndex.value < _questions.value.size - 1) {
            _currentIndex.value += 1
            _quizState.value = QuizState.PLAYING
            _currentInput.value = ""
            _isCorrect.value = false
            resetHintState()
        } else {
            _quizState.value = QuizState.FINISHED
            if (finishSoundId != 0) soundPool.play(finishSoundId, 1f, 1f, 0, 0, 1f)
            if (_mode.value == Mode.DAILY) recordDailyCompletion()
        }
    }

    /**
     * 오늘 몫을 끝냈다 → 출석을 찍는다.
     *
     * **오늘의 퀴즈를 끝까지 푼 것만 출석으로 친다**(앱 실행은 출석이 아니다).
     * 출석이 너무 쉬우면 연속일수가 아무 의미도 없어지고, 「오늘 다 했다」는 완결감도 안 생긴다.
     */
    private fun recordDailyCompletion() {
        viewModelScope.launch {
            try {
                val before = settingsRepository.dailyStatusFlow.first()
                val beforeBadges = com.kitwlshcom.kdailyutil.data.DailyRecord
                    .badges(before.streak, before.bestStreak, before.totalSolved, before.totalCorrect)
                    .filter { it.achieved }.map { it.id }.toSet()

                val after = settingsRepository.completeDailyQuiz(
                    correct = _score.value,
                    total = _questions.value.size
                )

                val newBadges = com.kitwlshcom.kdailyutil.data.DailyRecord
                    .badges(after.streak, after.bestStreak, after.totalSolved, after.totalCorrect)
                    .filter { it.achieved && it.id !in beforeBadges }

                _dailyCelebration.value = DailyCelebration(
                    streak = after.displayStreak(),
                    correct = _score.value,
                    total = _questions.value.size,
                    newBadges = newBadges
                )
                refreshCounters()
            } catch (e: Exception) {
                // 기록에 실패해도 결과 화면은 떠야 한다.
                Log.e("QuizViewModel", "출석 기록 실패: ${e.message}")
            }
        }
    }

    fun exitQuiz()
    {
        _mode.value = Mode.NORMAL
        _quizState.value = QuizState.CATEGORY_SELECTION
        _selectedCategory.value = null
    }

    fun enterCreator()
    {
        _quizState.value = QuizState.CREATOR
    }

    fun loadCategories()
    {
        viewModelScope.launch {
            val custom = repository.getCustomCategories(getApplication())
            val remote = repository.getRemoteCategories(getApplication())
            val baseList = listOf("우리말 겨루기", "트렌드 말하기", "상식 백과", "세계 여행", "AI 자동 생성 (KuizGenius)")
            
            _customCategories.value = custom
            val extraRemote = remote.filter { !baseList.contains(it) }
            
            _availableCategories.value = baseList + extraRemote + custom
        }
    }

    fun deleteCustomCategory(category: String)
    {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            repository.deleteCustomCategory(context, category)
            loadCategories()
        }
    }

    fun importQuizFromUri(uri: android.net.Uri)
    {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val pkg = com.kitwlshcom.kdailyutil.data.QuizFileHandler.importQuizzes(context, uri)
            if (pkg != null)
            {
                // 이미 동일한 이름의 카테고리가 존재하는지 검사
                if (_availableCategories.value.contains(pkg.category))
                {
                    _pendingImport.value = pkg
                }
                else
                {
                    repository.saveCustomQuizzes(context, pkg.questions)
                    loadCategories()
                    Log.d("QuizViewModel", "✅ Successfully imported custom category: ${pkg.category}")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "📥 [${pkg.category}] 카테고리 퀴즈(${pkg.questions.size}문제)를 성공적으로 가져왔습니다!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else
            {
                Log.e("QuizViewModel", "❌ Failed to import quiz package from Uri: $uri")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "❌ 퀴즈 패키지 가져오기에 실패했습니다. 올바른 .kquiz 형식의 파일인지 확인해주세요.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** AI가 파일이 아닌 텍스트(JSON)로 준 경우, 그 텍스트를 붙여넣어 가져온다. */
    fun importQuizFromText(rawText: String)
    {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val pkg = com.kitwlshcom.kdailyutil.data.QuizFileHandler.importQuizzesFromText(rawText, context)
            if (pkg != null)
            {
                if (_availableCategories.value.contains(pkg.category))
                {
                    _pendingImport.value = pkg
                }
                else
                {
                    repository.saveCustomQuizzes(context, pkg.questions)
                    loadCategories()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "📥 [${pkg.category}] 퀴즈(${pkg.questions.size}문제)를 붙여넣기로 가져왔습니다!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else
            {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "❌ 붙여넣은 내용에서 올바른 퀴즈(JSON)를 찾지 못했습니다. AI가 준 JSON 전체를 복사했는지 확인해주세요.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun resolveImportConflict(resolution: String, newName: String? = null) {
        val pkg = _pendingImport.value ?: return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            when (resolution) {
                "MERGE" -> {
                    repository.saveCustomQuizzes(context, pkg.questions)
                    loadCategories()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "📥 기존 [${pkg.category}] 카테고리에 문제를 성공적으로 합쳤습니다!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                "SEPARATE" -> {
                    val finalName = newName ?: "${pkg.category} (새 패키지)"
                    val renamedQuizzes = pkg.questions.map { q ->
                        q.copy(category = finalName, id = Math.abs((finalName + q.question).hashCode()))
                    }
                    repository.saveCustomQuizzes(context, renamedQuizzes)
                    loadCategories()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "📥 새로운 카테고리 [${finalName}]로 분리하여 성공적으로 저장했습니다!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                "OVERWRITE" -> {
                    repository.deleteCustomCategory(context, pkg.category)
                    repository.saveCustomQuizzes(context, pkg.questions)
                    loadCategories()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "📥 기존 [${pkg.category}] 카테고리를 삭제하고 새 문제로 성공적으로 덮어썼습니다!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            _pendingImport.value = null
        }
    }

    fun cancelImportConflict() {
        _pendingImport.value = null
    }

    fun saveCustomQuizzes(quizzes: List<QuizQuestion>)
    {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            repository.saveCustomQuizzes(context, quizzes)
            loadCategories()
        }
    }

    /**
     * 커스텀(개인) 문제를 폼으로 수정해 저장한다.
     * 같은 id로 저장하면 저장소가 기존 항목을 갱신하고, 진행 중 화면에도 즉시 반영한다.
     */
    fun updateCustomQuestion(updated: QuizQuestion)
    {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            repository.saveCustomQuizzes(context, listOf(updated))
            // 진행 중인 풀이 목록에도 즉시 반영 (같은 id 교체)
            _questions.value = _questions.value.map { if (it.id == updated.id) updated else it }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "문제를 수정했어요.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateWrongOptions(
        question: String,
        answer: String,
        onComplete: (options: List<String>?, explanation: String) -> Unit
    )
    {
        viewModelScope.launch {
            val apiKey = settingsRepository.geminiApiKeyFlow.first()
            if (apiKey.isNullOrBlank())
            {
                onComplete(null, "API 키가 필요합니다.")
                return@launch
            }

            try
            {
                val gemini = GeminiManager(apiKey)
                val jsonString = gemini.generateOptionsForQuestion(question, answer)
                if (jsonString.isNotBlank())
                {
                    val obj = org.json.JSONObject(jsonString)
                    val optionsArray = obj.getJSONArray("options")
                    val optionsList = List(optionsArray.length()) { optionsArray.getString(it) }
                    val explanation = obj.getString("explanation")
                    onComplete(optionsList, explanation)
                }
                else
                {
                    onComplete(null, "보기 생성에 실패했습니다.")
                }
            }
            catch (e: Exception)
            {
                Log.e("QuizViewModel", "❌ AI Option Generation Failed: ${e.message}", e)
                onComplete(null, "보기를 만들지 못했습니다.\n" + GeminiManager.aiErrorMessage(e))
            }
        }
    }

    fun generateAiQuizFromImages(
        images: List<android.graphics.Bitmap>,
        categoryName: String
    )
    {
        viewModelScope.launch {
            _quizState.value = QuizState.GENERATING
            val apiKey = settingsRepository.geminiApiKeyFlow.first()
            if (apiKey.isNullOrBlank())
            {
                _quizState.value = QuizState.CATEGORY_SELECTION
                return@launch
            }

            try
            {
                val context = getApplication<Application>().applicationContext
                val statsManager = com.kitwlshcom.kdailyutil.data.QuizStatsManager.getInstance(context)
                
                // Read previous questions to prevent duplicates
                val previousQuizzes = repository.getQuizzes(context)
                val prevQuizzesArray = JSONArray().apply {
                    previousQuizzes.forEach { q ->
                        put(JSONObject().apply {
                            put("question", q.question)
                            put("category", q.category)
                        })
                    }
                }
                
                // Fetch top error statistics
                val highErrorStats = statsManager.getHighErrorQuestions(5)
                val errorStatsArray = JSONArray().apply {
                    highErrorStats.forEach { (key, rate) ->
                        put(JSONObject().apply {
                            put("questionKey", key)
                            put("errorRate", rate)
                        })
                    }
                }

                val gemini = GeminiManager(apiKey)
                val jsonString = gemini.generateQuizzesFromImages(
                    images = images,
                    previousQuizzesJson = prevQuizzesArray.toString(),
                    errorStatsJson = errorStatsArray.toString()
                )

                if (jsonString.isBlank())
                {
                    Log.e("QuizViewModel", "❌ Empty response from image quiz generation")
                    _quizState.value = QuizState.CATEGORY_SELECTION
                    return@launch
                }

                val jsonArray = JSONArray(jsonString)
                val aiQuestions = mutableListOf<QuizQuestion>()

                for (i in 0 until jsonArray.length())
                {
                    val obj = jsonArray.getJSONObject(i)
                    val optionsArray = obj.optJSONArray("options")
                    val optionsList = if (optionsArray != null)
                    {
                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                    }
                    else null

                    val baseQuestion = obj.getString("question")
                    val uniqueId = Math.abs((categoryName + baseQuestion).hashCode())

                    aiQuestions.add(
                        QuizQuestion(
                            id = uniqueId,
                            type = QuizType.fromRaw(obj.optString("type"), optionsList != null),
                            category = categoryName,
                            subCategory = obj.optString("subCategory", "AI 이미지 분석"),
                            question = baseQuestion,
                            options = optionsList,
                            answer = obj.getString("answer"),
                            explanation = obj.getString("explanation"),
                            semanticHint = obj.optString("semanticHint", null)
                        )
                    )
                }

                repository.saveCustomQuizzes(context, aiQuestions)
                loadCategories()
                
                _questions.value = aiQuestions
                _currentIndex.value = 0
                _score.value = 0
                _quizState.value = QuizState.PLAYING
                _currentInput.value = ""
                _isCorrect.value = false
                resetHintState()
            }
            catch (e: Exception)
            {
                Log.e("QuizViewModel", "❌ AI Image Quiz Generation Failed: ${e.message}", e)
                _quizState.value = QuizState.CATEGORY_SELECTION
            }
        }
    }
}
