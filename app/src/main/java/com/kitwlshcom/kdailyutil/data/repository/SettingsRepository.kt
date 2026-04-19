package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEYWORDS = stringSetPreferencesKey("keywords")
        val BRIEFING_HOUR = intPreferencesKey("briefing_hour")
        val BRIEFING_MINUTE = intPreferencesKey("briefing_minute")
        val IS_BRIEFING_ENABLED = booleanPreferencesKey("is_briefing_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val keywordsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KEYWORDS] ?: emptySet()
    }

    val briefingTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        val hour = preferences[PreferencesKeys.BRIEFING_HOUR] ?: 7 // 기본값 오전 7시
        val minute = preferences[PreferencesKeys.BRIEFING_MINUTE] ?: 0
        Pair(hour, minute)
    }

    val isBriefingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_BRIEFING_ENABLED] ?: false
    }

    val geminiApiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY]
    }

    suspend fun updateKeywords(keywords: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEYWORDS] = keywords
        }
    }

    suspend fun updateBriefingTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BRIEFING_HOUR] = hour
            preferences[PreferencesKeys.BRIEFING_MINUTE] = minute
        }
    }

    suspend fun setBriefingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BRIEFING_ENABLED] = enabled
        }
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey
        }
    }
}
