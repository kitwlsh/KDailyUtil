package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingDataStore by preferencesDataStore(name = "reading_training")

/**
 * 빠른 독서 훈련 진척(최고 WPM / 연속일 / 누적 세션)을 DataStore에 영속 저장.
 */
class ReadingTrainingRepository(private val context: Context) {

    private object Keys {
        val BEST_WPM = intPreferencesKey("best_wpm")
        val STREAK = intPreferencesKey("streak_days")
        val LAST_DATE = stringPreferencesKey("last_trained_date") // yyyyMMdd
        val TOTAL = intPreferencesKey("total_sessions")
    }

    val bestWpmFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.BEST_WPM] ?: 0 }
    val streakFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.STREAK] ?: 0 }
    val totalSessionsFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.TOTAL] ?: 0 }

    /**
     * 한 세션 완료 기록. wpm=0이면 최고 WPM은 갱신하지 않음(워밍업 등).
     * @param today,yesterday yyyyMMdd 문자열 (연속일 계산용)
     */
    suspend fun recordSession(wpm: Int, today: String, yesterday: String) {
        context.readingDataStore.edit { p ->
            val prevBest = p[Keys.BEST_WPM] ?: 0
            if (wpm > prevBest) p[Keys.BEST_WPM] = wpm
            p[Keys.TOTAL] = (p[Keys.TOTAL] ?: 0) + 1
            val last = p[Keys.LAST_DATE] ?: ""
            val streak = p[Keys.STREAK] ?: 0
            p[Keys.STREAK] = when (last) {
                today -> if (streak <= 0) 1 else streak
                yesterday -> streak + 1
                else -> 1
            }
            p[Keys.LAST_DATE] = today
        }
    }
}
