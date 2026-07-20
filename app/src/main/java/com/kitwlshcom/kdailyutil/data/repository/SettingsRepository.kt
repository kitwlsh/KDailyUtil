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
}
