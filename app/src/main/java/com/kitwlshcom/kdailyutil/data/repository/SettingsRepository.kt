package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val EDIT_LOCKED = booleanPreferencesKey("edit_locked")
        
        // 기존 브리핑 관련 키 복구
        val KEYWORDS = stringSetPreferencesKey("keywords")
        val BRIEFING_HOUR = intPreferencesKey("briefing_hour")
        val BRIEFING_MINUTE = intPreferencesKey("briefing_minute")
        val BRIEFING_ENABLED = booleanPreferencesKey("briefing_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val DART_API_KEY = stringPreferencesKey("dart_api_key")
        val NEWS_CATEGORIES = stringSetPreferencesKey("news_categories")
        val AI_BRIEFING_COMMAND = stringPreferencesKey("ai_briefing_command") // 하위 호환용
        val AI_BRIEFING_COMMANDS = stringSetPreferencesKey("ai_briefing_commands") // 다중 커맨드용(레거시 Set)
        val AI_COMMAND_AUDIO_PATH = stringPreferencesKey("ai_command_audio_path")
        val IS_API_KEY_VALIDATED = booleanPreferencesKey("is_api_key_validated")
        val STOCK_KEYWORDS = stringSetPreferencesKey("stock_keywords")       // 📰 뉴스탭 증시 필터 키워드(레거시 Set)
        val WATCH_STOCK_KEYWORDS = stringSetPreferencesKey("watch_stock_keywords") // 📈 증시탭 종목 관심목록(레거시 Set)
        val AUTO_REFRESH_INTERVAL_HOURS = intPreferencesKey("auto_refresh_interval_hours")
        val NEWS_LIMIT = intPreferencesKey("news_limit")
        val SPLASH_THEME = stringPreferencesKey("splash_theme")
        val AUDIO_COPYRIGHT_ACCEPTED = booleanPreferencesKey("audio_copyright_accepted")

        // ── 순서 보존(Set→List) 저장용 키 (2026-07-20) ──
        // DataStore Preferences는 List 네이티브 타입이 없어, 순서 있는 목록을 구분자(\n)로 이은
        // 문자열로 저장한다. 값이 없으면(null) 위의 레거시 Set 키에서 1회 마이그레이션해 읽는다.
        val KEYWORDS_ORDER = stringPreferencesKey("keywords_order")
        val NEWS_CATEGORIES_ORDER = stringPreferencesKey("news_categories_order")
        val AI_BRIEFING_COMMANDS_ORDER = stringPreferencesKey("ai_briefing_commands_order")
        val STOCK_KEYWORDS_ORDER = stringPreferencesKey("stock_keywords_order")
        val WATCH_STOCK_KEYWORDS_ORDER = stringPreferencesKey("watch_stock_keywords_order")

        // ── 🔁 매일 오게 하는 장치(출석·연속·기록) 2026-09-04 ──
        // ⚠️ 이 키들의 의미는 한 번 출시하면 되돌리기 어렵다. 사용자 기기에 이 규칙대로 기록이 쌓인다.
        //    규칙 자체는 DailyRecord(순수 로직)에 있고 DailyRecordTest가 고정한다.
        val DAILY_LAST_DONE = stringPreferencesKey("daily_last_done")       // 마지막으로 '오늘의 퀴즈'를 끝낸 날(yyyy-MM-dd)
        val DAILY_STREAK = intPreferencesKey("daily_streak")                // 그날 기준 연속일수
        val DAILY_BEST_STREAK = intPreferencesKey("daily_best_streak")      // 최고 기록(배지 판정은 이것으로 — 끊겼다고 뺏지 않는다)
        val DAILY_LAST_FREEZE = stringPreferencesKey("daily_last_freeze")   // 유예를 마지막으로 쓴 날
        val DAILY_TOTAL_SOLVED = intPreferencesKey("daily_total_solved")    // 누적 풀이 문항
        val DAILY_TOTAL_CORRECT = intPreferencesKey("daily_total_correct")  // 누적 정답
        val DAILY_HISTORY = stringPreferencesKey("daily_history")           // 최근 30일 "yyyy-MM-dd:정답/전체"
        val DAILY_SEEN_QUIZ_COUNT = intPreferencesKey("daily_seen_quiz_count") // 마지막으로 사용자가 인지한 전체 문항 수(=새 문제 N개 계산용)
    }

    /**
     * 순서 보존 목록 읽기: 새 순서 키(String, \n 구분)가 있으면 그걸 쓰고,
     * 없으면 레거시 Set 키에서 마이그레이션(순서는 임의지만 항목은 보존), 둘 다 없으면 기본값.
     * 새 키가 빈 문자열("")이면 "사용자가 전부 지운 상태"로 보고 빈 목록을 반환한다.
     */
    private fun readOrderedList(
        preferences: Preferences,
        orderKey: Preferences.Key<String>,
        legacySetKey: Preferences.Key<Set<String>>,
        default: List<String>
    ): List<String> {
        val ordered = preferences[orderKey]
        if (ordered != null) return ordered.split("\n").filter { it.isNotBlank() }
        val legacy = preferences[legacySetKey]
        if (legacy != null) return legacy.toList()
        return default
    }

    // 신규 오디오 설정
    val playbackModeFlow: Flow<PlaybackMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.PLAYBACK_MODE] ?: PlaybackMode.SEQUENTIAL.name
        try { PlaybackMode.valueOf(modeName) } catch (e: Exception) { PlaybackMode.SEQUENTIAL }
    }

    val isEditLockedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EDIT_LOCKED] ?: true
    }

    val audioCopyrightAcceptedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIO_COPYRIGHT_ACCEPTED] ?: false
    }

    // 기존 브리핑 설정 Flow 복구 (순서 보존 List)
    val keywordsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        readOrderedList(preferences, PreferencesKeys.KEYWORDS_ORDER, PreferencesKeys.KEYWORDS, emptyList())
    }

    val briefingTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        val hour = preferences[PreferencesKeys.BRIEFING_HOUR] ?: 7
        val minute = preferences[PreferencesKeys.BRIEFING_MINUTE] ?: 0
        Pair(hour, minute)
    }

    val isBriefingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BRIEFING_ENABLED] ?: false
    }

    val geminiApiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY]
    }
    
    val dartApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DART_API_KEY] ?: com.kitwlshcom.kdailyutil.BuildConfig.DART_DEFAULT_KEY
    }

    val categoriesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        readOrderedList(
            preferences, PreferencesKeys.NEWS_CATEGORIES_ORDER, PreferencesKeys.NEWS_CATEGORIES,
            listOf("전체", "정치", "경제", "증시", "사회", "IT/과학", "세계")
        )
    }

    val aiBriefingCommandFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_BRIEFING_COMMAND] ?: ""
    }

    val aiBriefingCommandsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        readOrderedList(preferences, PreferencesKeys.AI_BRIEFING_COMMANDS_ORDER, PreferencesKeys.AI_BRIEFING_COMMANDS, emptyList())
    }

    /**
     * 📰 뉴스탭 증시 뉴스 필터 키워드 (MorningBriefingSettingsScreen / BriefingViewModel 에서 사용)
     */
    val stockKeywordsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        readOrderedList(
            preferences, PreferencesKeys.STOCK_KEYWORDS_ORDER, PreferencesKeys.STOCK_KEYWORDS,
            listOf("나스닥", "코스피", "테슬라", "비트코인")
        )
    }

    /**
     * 📈 증시탭 관심종목 목록 (StockViewModel / StockDashboardScreen 에서 사용)
     * 뉴스 필터 키워드(stockKeywordsFlow)와 완전히 독립된 별도 저장소
     */
    val watchStockKeywordsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        readOrderedList(
            preferences, PreferencesKeys.WATCH_STOCK_KEYWORDS_ORDER, PreferencesKeys.WATCH_STOCK_KEYWORDS,
            listOf("나스닥", "코스피", "테슬라", "비트코인") // 앱 최초 실행 기본값
        )
    }

    val aiCommandAudioPathFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_COMMAND_AUDIO_PATH] ?: ""
    }

    val isApiKeyValidatedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_API_KEY_VALIDATED] ?: false
    }

    val autoRefreshIntervalFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_REFRESH_INTERVAL_HOURS] ?: 2 // 기본값 2시간
    }

    val newsLimitFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NEWS_LIMIT] ?: 20 // 기본값 20개
    }

    val splashThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SPLASH_THEME] ?: "shimmer"
    }

    // 저장 메서드들
    suspend fun savePlaybackMode(mode: PlaybackMode) {
        context.dataStore.edit { it[PreferencesKeys.PLAYBACK_MODE] = mode.name }
    }

    suspend fun saveEditLocked(isLocked: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.EDIT_LOCKED] = isLocked }
    }

    suspend fun saveAudioCopyrightAccepted(accepted: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_COPYRIGHT_ACCEPTED] = accepted }
    }

    /** \n 구분 순서 문자열로 저장(중복은 첫 등장만 유지해 Set 유일성 보존) */
    private suspend fun saveOrderedList(orderKey: Preferences.Key<String>, items: List<String>) {
        context.dataStore.edit {
            it[orderKey] = items.map { s -> s.trim() }.filter { s -> s.isNotBlank() }.distinct().joinToString("\n")
        }
    }

    suspend fun updateKeywords(newKeywords: List<String>) {
        saveOrderedList(PreferencesKeys.KEYWORDS_ORDER, newKeywords)
    }

    suspend fun updateBriefingTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.BRIEFING_HOUR] = hour
            it[PreferencesKeys.BRIEFING_MINUTE] = minute
        }
    }

    suspend fun setBriefingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BRIEFING_ENABLED] = enabled }
    }

    suspend fun updateGeminiApiKey(key: String) {
        context.dataStore.edit { it[PreferencesKeys.GEMINI_API_KEY] = key }
    }

    suspend fun updateDartApiKey(key: String) {
        context.dataStore.edit { it[PreferencesKeys.DART_API_KEY] = key }
    }

    suspend fun updateCategories(categories: List<String>) {
        saveOrderedList(PreferencesKeys.NEWS_CATEGORIES_ORDER, categories)
    }

    suspend fun updateAiBriefingCommand(command: String) {
        context.dataStore.edit { it[PreferencesKeys.AI_BRIEFING_COMMAND] = command }
    }

    suspend fun updateAiBriefingCommands(commands: List<String>) {
        saveOrderedList(PreferencesKeys.AI_BRIEFING_COMMANDS_ORDER, commands)
    }

    suspend fun updateAiCommandAudioPath(path: String) {
        context.dataStore.edit { it[PreferencesKeys.AI_COMMAND_AUDIO_PATH] = path }
    }

    suspend fun setApiKeyValidated(isValidated: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_API_KEY_VALIDATED] = isValidated }
    }

    /** 📰 뉴스탭 증시 뉴스 필터 키워드 저장 (순서 보존) */
    suspend fun updateStockKeywords(keywords: List<String>) {
        saveOrderedList(PreferencesKeys.STOCK_KEYWORDS_ORDER, keywords)
    }

    /** 📈 증시탭 관심종목 목록 저장 (순서 보존) */
    suspend fun updateWatchStockKeywords(keywords: List<String>) {
        saveOrderedList(PreferencesKeys.WATCH_STOCK_KEYWORDS_ORDER, keywords)
    }

    suspend fun updateAutoRefreshInterval(hours: Int) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_REFRESH_INTERVAL_HOURS] = hours }
    }

    suspend fun updateNewsLimit(limit: Int) {
        context.dataStore.edit { it[PreferencesKeys.NEWS_LIMIT] = limit }
    }

    suspend fun updateSplashTheme(theme: String) {
        context.dataStore.edit { it[PreferencesKeys.SPLASH_THEME] = theme }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔁 출석·연속·기록 (2026-09-04)
    //
    // 규칙은 여기 두지 않는다 — 전부 DailyRecord(순수 로직)에 있고 테스트로 고정돼 있다.
    // 이 클래스는 «읽고 쓰는 일»만 한다.
    // ──────────────────────────────────────────────────────────────

    /** 화면 한 번 그리는 데 필요한 출석 정보 전부. 조각조각 Flow를 만들면 화면에서 다시 합쳐야 한다. */
    data class DailyStatus(
        val lastDone: java.time.LocalDate? = null,
        val streak: Int = 0,
        val bestStreak: Int = 0,
        val lastFreeze: java.time.LocalDate? = null,
        val totalSolved: Int = 0,
        val totalCorrect: Int = 0,
        val history: List<com.kitwlshcom.kdailyutil.data.DailyRecord.DayScore> = emptyList(),
        val seenQuizCount: Int = 0
    ) {
        /** 오늘 몫을 이미 끝냈는가. */
        fun isDoneToday(today: java.time.LocalDate = java.time.LocalDate.now()): Boolean = lastDone == today

        /** 화면에 보여 줄 연속일수(저장값을 그대로 쓰면 쉰 사람에게 거짓말이 된다). */
        fun displayStreak(today: java.time.LocalDate = java.time.LocalDate.now()): Int =
            com.kitwlshcom.kdailyutil.data.DailyRecord.displayStreak(today, lastDone, streak)
    }

    /** 저장된 날짜 문자열을 LocalDate로. 깨져 있으면 null(기록 하나 때문에 화면이 죽으면 안 된다). */
    private fun parseDate(raw: String?): java.time.LocalDate? = try {
        if (raw.isNullOrBlank()) null else java.time.LocalDate.parse(raw)
    } catch (e: Exception) {
        null
    }

    val dailyStatusFlow: Flow<DailyStatus> = context.dataStore.data.map { preferences ->
        DailyStatus(
            lastDone = parseDate(preferences[PreferencesKeys.DAILY_LAST_DONE]),
            streak = preferences[PreferencesKeys.DAILY_STREAK] ?: 0,
            bestStreak = preferences[PreferencesKeys.DAILY_BEST_STREAK] ?: 0,
            lastFreeze = parseDate(preferences[PreferencesKeys.DAILY_LAST_FREEZE]),
            totalSolved = preferences[PreferencesKeys.DAILY_TOTAL_SOLVED] ?: 0,
            totalCorrect = preferences[PreferencesKeys.DAILY_TOTAL_CORRECT] ?: 0,
            history = com.kitwlshcom.kdailyutil.data.DailyRecord.decodeHistory(preferences[PreferencesKeys.DAILY_HISTORY]),
            seenQuizCount = preferences[PreferencesKeys.DAILY_SEEN_QUIZ_COUNT] ?: 0
        )
    }

    /**
     * 오늘의 퀴즈를 끝냈다 — 출석을 찍고 연속을 갱신한다.
     *
     * 누적 문항/정답은 **연속과 무관하게 항상** 더한다(하루에 두 번 풀어도 푼 것은 푼 것이다).
     * 반면 연속·기록은 [com.kitwlshcom.kdailyutil.data.DailyRecord.advance] 규칙을 따른다.
     *
     * @return 갱신 후 상태(화면이 «연속 7일!»을 바로 띄울 수 있게)
     */
    suspend fun completeDailyQuiz(
        correct: Int,
        total: Int,
        today: java.time.LocalDate = java.time.LocalDate.now()
    ): DailyStatus {
        val daily = com.kitwlshcom.kdailyutil.data.DailyRecord
        var result = DailyStatus()
        context.dataStore.edit { prefs ->
            val lastDone = parseDate(prefs[PreferencesKeys.DAILY_LAST_DONE])
            val streak = prefs[PreferencesKeys.DAILY_STREAK] ?: 0
            val lastFreeze = parseDate(prefs[PreferencesKeys.DAILY_LAST_FREEZE])

            val advanced = daily.advance(today, lastDone, streak, lastFreeze)
            val best = maxOf(prefs[PreferencesKeys.DAILY_BEST_STREAK] ?: 0, advanced.streak)

            val totalSolved = (prefs[PreferencesKeys.DAILY_TOTAL_SOLVED] ?: 0) + total
            val totalCorrect = (prefs[PreferencesKeys.DAILY_TOTAL_CORRECT] ?: 0) + correct

            val history = daily.upsertHistory(
                daily.decodeHistory(prefs[PreferencesKeys.DAILY_HISTORY]),
                com.kitwlshcom.kdailyutil.data.DailyRecord.DayScore(today, correct, total)
            )

            prefs[PreferencesKeys.DAILY_LAST_DONE] = today.toString()
            prefs[PreferencesKeys.DAILY_STREAK] = advanced.streak
            prefs[PreferencesKeys.DAILY_BEST_STREAK] = best
            advanced.lastFreeze?.let { prefs[PreferencesKeys.DAILY_LAST_FREEZE] = it.toString() }
            prefs[PreferencesKeys.DAILY_TOTAL_SOLVED] = totalSolved
            prefs[PreferencesKeys.DAILY_TOTAL_CORRECT] = totalCorrect
            prefs[PreferencesKeys.DAILY_HISTORY] = daily.encodeHistory(history)

            result = DailyStatus(
                lastDone = today,
                streak = advanced.streak,
                bestStreak = best,
                lastFreeze = advanced.lastFreeze,
                totalSolved = totalSolved,
                totalCorrect = totalCorrect,
                history = history,
                seenQuizCount = prefs[PreferencesKeys.DAILY_SEEN_QUIZ_COUNT] ?: 0
            )
        }
        return result
    }

    /** 「새 문제 N개」를 계산하는 기준점. 사용자가 문제 목록을 본 시점에 갱신한다. */
    suspend fun updateSeenQuizCount(count: Int) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_SEEN_QUIZ_COUNT] = count }
    }
}
