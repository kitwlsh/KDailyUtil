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
        val NEWS_CATEGORIES = stringSetPreferencesKey("news_categories")
        val AI_BRIEFING_COMMAND = stringPreferencesKey("ai_briefing_command")
        val AI_COMMAND_AUDIO_PATH = stringPreferencesKey("ai_command_audio_path")
    }

    // 신규 오디오 설정
    val playbackModeFlow: Flow<PlaybackMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.PLAYBACK_MODE] ?: PlaybackMode.SEQUENTIAL.name
        try { PlaybackMode.valueOf(modeName) } catch (e: Exception) { PlaybackMode.SEQUENTIAL }
    }

    val isEditLockedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EDIT_LOCKED] ?: true
    }

    // 기존 브리핑 설정 Flow 복구
    val keywordsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEYWORDS] ?: emptySet()
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

    val categoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NEWS_CATEGORIES] ?: setOf("전체", "정치", "경제", "사회", "IT/과학", "세계")
    }

    val aiBriefingCommandFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_BRIEFING_COMMAND] ?: ""
    }

    val aiCommandAudioPathFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_COMMAND_AUDIO_PATH] ?: ""
    }

    // 저장 메서드들
    suspend fun savePlaybackMode(mode: PlaybackMode) {
        context.dataStore.edit { it[PreferencesKeys.PLAYBACK_MODE] = mode.name }
    }

    suspend fun saveEditLocked(isLocked: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.EDIT_LOCKED] = isLocked }
    }

    suspend fun updateKeywords(newKeywords: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.KEYWORDS] = newKeywords }
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

    suspend fun updateCategories(categories: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.NEWS_CATEGORIES] = categories }
    }

    suspend fun updateAiBriefingCommand(command: String) {
        context.dataStore.edit { it[PreferencesKeys.AI_BRIEFING_COMMAND] = command }
    }

    suspend fun updateAiCommandAudioPath(path: String) {
        context.dataStore.edit { it[PreferencesKeys.AI_COMMAND_AUDIO_PATH] = path }
    }
}
