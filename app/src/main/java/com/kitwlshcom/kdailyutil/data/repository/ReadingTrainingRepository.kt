package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val Context.readingDataStore by preferencesDataStore(name = "reading_training")

/** 보관함에 저장된 연습 지문 (촬영/붙여넣기) */
data class SavedPassage(
    val id: String,
    val title: String,
    val text: String,
    val imagePath: String?,
    val createdAt: Long
)

/**
 * 빠른 독서 훈련 진척(최고 WPM / 연속일 / 누적 세션)을 DataStore에 영속 저장.
 */
class ReadingTrainingRepository(private val context: Context) {

    private object Keys {
        val BEST_WPM = intPreferencesKey("best_wpm")
        val STREAK = intPreferencesKey("streak_days")
        val LAST_DATE = stringPreferencesKey("last_trained_date") // yyyyMMdd
        val TOTAL = intPreferencesKey("total_sessions")
        val BEST_COMPREHENSION = intPreferencesKey("best_comprehension") // 0~100
        val TRAINED_DATES = stringSetPreferencesKey("trained_dates") // yyyyMMdd 집합
    }

    val bestWpmFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.BEST_WPM] ?: 0 }
    val streakFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.STREAK] ?: 0 }
    val totalSessionsFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.TOTAL] ?: 0 }
    val bestComprehensionFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.BEST_COMPREHENSION] ?: 0 }
    val trainedDatesFlow: Flow<Set<String>> = context.readingDataStore.data.map { it[Keys.TRAINED_DATES] ?: emptySet() }

    /** 이해도 점수(0~100) 기록 — 최고치만 갱신 */
    suspend fun recordComprehension(scorePercent: Int) {
        context.readingDataStore.edit { p ->
            val prev = p[Keys.BEST_COMPREHENSION] ?: 0
            if (scorePercent > prev) p[Keys.BEST_COMPREHENSION] = scorePercent
        }
    }

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
            // 훈련일 집합에 오늘 추가 (21일 챌린지용)
            p[Keys.TRAINED_DATES] = (p[Keys.TRAINED_DATES] ?: emptySet()) + today
        }
    }

    // ── WPM 추이 이력 (최근 30회, 파일 기반) ──────────────────
    private val wpmHistoryFile: File get() = File(context.filesDir, "reading_wpm_history.json")

    @Synchronized
    fun loadWpmHistory(): List<Int> {
        if (!wpmHistoryFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(wpmHistoryFile.readText())
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (e: Exception) { emptyList() }
    }

    @Synchronized
    fun addWpmHistory(wpm: Int) {
        try {
            val list = loadWpmHistory().toMutableList()
            list.add(wpm)
            while (list.size > 30) list.removeAt(0)
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            wpmHistoryFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("ReadingRepo", "addWpmHistory 실패: ${e.message}")
        }
    }

    // ── 지문 보관함 (파일 기반) ────────────────────────────────
    private val passagesFile: File get() = File(context.filesDir, "reading_passages.json")
    private val pagesDir: File get() = File(context.filesDir, "reading_pages").apply { if (!exists()) mkdirs() }

    @Synchronized
    fun loadPassages(): List<SavedPassage> {
        if (!passagesFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(passagesFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SavedPassage(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    imagePath = o.optString("imagePath").takeIf { it.isNotBlank() },
                    createdAt = o.optLong("createdAt")
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("ReadingRepo", "loadPassages 실패: ${e.message}"); emptyList()
        }
    }

    @Synchronized
    private fun saveAll(list: List<SavedPassage>) {
        try {
            val arr = JSONArray()
            list.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id); put("title", p.title); put("text", p.text)
                    put("imagePath", p.imagePath ?: ""); put("createdAt", p.createdAt)
                })
            }
            passagesFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("ReadingRepo", "saveAll 실패: ${e.message}")
        }
    }

    /** 비트맵을 reading_pages/에 JPEG로 저장하고 경로 반환 (썸네일/원본용) */
    fun saveImage(bitmap: Bitmap): String? = try {
        val f = File(pagesDir, "page_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        f.absolutePath
    } catch (e: Exception) {
        Log.e("ReadingRepo", "saveImage 실패: ${e.message}"); null
    }

    fun addPassage(text: String, imagePath: String?, now: Long): SavedPassage {
        val title = text.trim().take(24).replace("\n", " ").ifBlank { "지문" }
        val item = SavedPassage(UUID.randomUUID().toString(), title, text.trim(), imagePath, now)
        saveAll(listOf(item) + loadPassages())
        return item
    }

    fun deletePassage(id: String) {
        val target = loadPassages().find { it.id == id }
        target?.imagePath?.let { runCatching { File(it).delete() } }
        saveAll(loadPassages().filter { it.id != id })
    }

    /** 보관함 지문의 제목만 변경(본문·이미지·생성시각은 유지). 빈 제목은 무시. */
    fun renamePassage(id: String, newTitle: String) {
        val clean = newTitle.trim().replace("\n", " ").take(40)
        if (clean.isBlank()) return
        saveAll(loadPassages().map { if (it.id == id) it.copy(title = clean) else it })
    }
}
