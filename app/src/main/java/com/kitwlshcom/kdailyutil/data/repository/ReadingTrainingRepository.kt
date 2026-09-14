package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kitwlshcom.kdailyutil.data.ReadingTrainingModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID

private val Context.readingDataStore by preferencesDataStore(name = "reading_training")

/**
 * 로봇이 매일 넣어 주는 원격 지문 (2026-09-07).
 *
 * 🔴 사용자 보관함([SavedPassage])과 **다른 타입·다른 파일**이다. 섞으면 동기화 버그 한 번에
 * 사용자가 촬영해 넣은 지문이 날아간다 — 되돌릴 수 없는 종류의 사고다.
 */
data class RemotePassage(
    val id: Long,
    val title: String,
    val theme: String,
    val text: String,
    /** 도착일. 「새 지문 N편」의 신규 창(7일) 판정에 쓴다. 깨져 있으면 null. */
    val createdAt: LocalDate?
)

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
        // 「새 지문 N편」의 기준점 = 사용자가 마지막으로 인지한 원격 지문 총 편수
        val SEEN_PASSAGE_COUNT = intPreferencesKey("seen_passage_count")
        // 사용자가 목록에서 치운 로봇 지문 id. 🔴 원본은 지우지 않는다(«내가 못 본 것이 지워졌다»는 인상을 주지 않는다)
        val HIDDEN_REMOTE_IDS = stringSetPreferencesKey("hidden_remote_passage_ids")
        // 사용자가 정한 «기본 훈련». 지문 카드의 시작 버튼이 이것으로 시작한다.
        // 🔴 **키 문자열은 그대로 둔다**("last_training_module") — 2026-09-14에 의미만 바뀌었다
        //    («마지막에 한 것을 앱이 기억» → «사용자가 정한 기본»). 키를 바꾸면 기존 사용자의
        //    값이 통째로 날아가 전원이 리듬 페이서로 되돌아간다.
        val LAST_MODULE = stringPreferencesKey("last_training_module")
    }

    val bestWpmFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.BEST_WPM] ?: 0 }
    val streakFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.STREAK] ?: 0 }
    val totalSessionsFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.TOTAL] ?: 0 }
    val bestComprehensionFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.BEST_COMPREHENSION] ?: 0 }
    val trainedDatesFlow: Flow<Set<String>> = context.readingDataStore.data.map { it[Keys.TRAINED_DATES] ?: emptySet() }
    val seenPassageCountFlow: Flow<Int> = context.readingDataStore.data.map { it[Keys.SEEN_PASSAGE_COUNT] ?: 0 }
    val hiddenRemoteIdsFlow: Flow<Set<String>> = context.readingDataStore.data.map { it[Keys.HIDDEN_REMOTE_IDS] ?: emptySet() }

    /** 마지막 훈련일(yyyyMMdd). 복귀 사면 판정에 쓴다 — 퀴즈 출석과 별개의 기록이다. */
    val lastTrainedDateFlow: Flow<String?> = context.readingDataStore.data.map { it[Keys.LAST_DATE] }

    /**
     * 사용자가 정한 **기본 훈련**(2026-09-14). 정한 적이 없으면 [ReadingTrainingModule.DEFAULT].
     *
     * 🔴 **예전에는 «마지막에 한 훈련»을 앱이 자동으로 기억했다** — 그런데 기억이 바뀌는 자리가
     * 네 곳이나 됐고, 그중 **결과 화면의 「▶ 다음: ○○」**은 «이어서 한 판 더»라는 뜻인데도
     * 기본 훈련을 통째로 갈아치웠다. 사용자는 바꾼 적이 없는데 **모든 지문의 시작 버튼이
     * 바뀌어 있었다.** 앱이 «추측»하는 대신 사용자가 «선언»하게 바꾼 것이 이 값이다.
     *
     * 저장된 값이 알 수 없는 문자열이면(손상·예전 값) 기본값으로 떨어진다 — 화면이 비지 않는다.
     */
    val defaultModuleFlow: Flow<ReadingTrainingModule> =
        context.readingDataStore.data.map {
            ReadingTrainingModule.fromKey(it[Keys.LAST_MODULE]) ?: ReadingTrainingModule.DEFAULT
        }

    /**
     * 기본 훈련을 바꾼다. 🔴 **이 함수를 부르는 자리는 «⭐ 기본으로 정하기» 한 곳뿐이어야 한다.**
     * 훈련을 시작하는 것만으로 여기를 부르면, 고치려던 그 문제(몰래 바뀜)가 그대로 돌아온다.
     */
    suspend fun setDefaultModule(module: ReadingTrainingModule) {
        context.readingDataStore.edit { p -> p[Keys.LAST_MODULE] = module.key }
    }

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

    // ── 원격 지문 (로봇 공급 · 2026-09-07) ──────────────────────
    //
    // 🔴 사용자 보관함 파일(reading_passages.json)에는 **절대 쓰지 않는다**.
    // 로봇 지문은 «갱신·교체»되는 물건이고 보관함은 «사용자가 찍어서 넣은» 물건이다.
    // 섞어 두면 동기화 버그 한 번에 사용자 데이터가 날아간다 — 되돌릴 수 없다.

    private val remotePassagesFile: File get() = File(context.filesDir, "reading_passages_remote.json")

    @Synchronized
    fun loadRemotePassages(): List<RemotePassage> {
        if (!remotePassagesFile.exists()) return emptyList()
        return try {
            parseRemotePassages(remotePassagesFile.readText())
        } catch (e: Exception) {
            // 캐시가 깨졌어도 화면은 살아야 한다 — 호출자가 내장 지문으로 떨어진다.
            Log.e("ReadingRepo", "원격 지문 캐시 파싱 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 로봇이 올린 지문을 받아 캐시에 넣는다.
     *
     * 🔴 **하나도 못 받으면 기존 캐시를 건드리지 않는다**(퀴즈의 last-good 규칙 그대로).
     * 통신이 안 되는 날 목록이 비어 버리면 사용자에게는 «지문이 사라진 앱»이 된다.
     *
     * 연도별 파일 2개(작년·올해)만 받는다 — 앱은 조건부 요청을 하지 않아 매번 파일 전체를
     * 다시 받는다. 한 파일에 계속 쌓으면 퀴즈처럼 매 동기화가 수백 KB가 된다.
     */
    suspend fun syncRemotePassages(today: LocalDate = LocalDate.now()) = withContext(Dispatchers.IO) {
        val fetched = LinkedHashMap<Long, RemotePassage>()
        var anySuccess = false

        for (year in listOf(today.year - 1, today.year)) {
            val fileName = "passages_$year.json"
            try {
                val connection = (URL(REMOTE_BASE_URL + fileName).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                when (connection.responseCode) {
                    200 -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        parseRemotePassages(body).forEach { fetched[it.id] = it }
                        anySuccess = true
                        Log.d("ReadingRepo", "✅ 지문 동기화 $fileName")
                    }
                    // 작년 파일은 원래 없을 수 있다(서비스 첫 해) → 실패가 아니다.
                    404 -> Log.d("ReadingRepo", "지문 파일 없음(정상): $fileName")
                    else -> Log.w("ReadingRepo", "지문 동기화 응답 ${connection.responseCode}: $fileName")
                }
            } catch (e: Exception) {
                Log.e("ReadingRepo", "❌ 지문 동기화 실패 $fileName: ${e.message}")
            }
        }

        if (!anySuccess) {
            Log.w("ReadingRepo", "⚠️ 받은 지문 파일이 없어 기존 캐시를 유지합니다.")
            return@withContext
        }

        try {
            val arr = JSONArray()
            fetched.values.sortedBy { it.id }.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("theme", p.theme)
                    put("text", p.text)
                    put("createdAt", p.createdAt?.toString() ?: "")
                })
            }
            remotePassagesFile.writeText(arr.toString())
            Log.d("ReadingRepo", "지문 캐시 저장: ${fetched.size}편")
        } catch (e: Exception) {
            Log.e("ReadingRepo", "지문 캐시 저장 실패: ${e.message}")
        }
    }

    private fun parseRemotePassages(raw: String): List<RemotePassage> {
        val arr = JSONArray(raw)
        val list = mutableListOf<RemotePassage>()
        for (i in 0 until arr.length()) {
            // 한 편이 깨졌다고 전체를 버리지 않는다 — 그 한 편만 조용히 건너뛴다.
            try {
                val o = arr.getJSONObject(i)
                val text = o.optString("text").trim()
                if (text.isBlank()) continue
                list.add(
                    RemotePassage(
                        id = o.optLong("id"),
                        title = o.optString("title").ifBlank { text.take(12) },
                        theme = o.optString("theme"),
                        text = text,
                        createdAt = runCatching { LocalDate.parse(o.optString("createdAt")) }.getOrNull()
                    )
                )
            } catch (e: Exception) {
                Log.w("ReadingRepo", "지문 한 편 건너뜀(${e.message})")
            }
        }
        return list
    }

    /** 「새 지문 N편」의 기준점을 지금으로 옮긴다(사용자가 목록을 본 시점). */
    suspend fun updateSeenPassageCount(count: Int) {
        context.readingDataStore.edit { it[Keys.SEEN_PASSAGE_COUNT] = count }
    }

    /** 목록에서 치운다. 🔴 원본은 지우지 않는다 — 다음 동기화에 되살아나는 것이 정상이다. */
    suspend fun hideRemotePassage(id: Long) {
        context.readingDataStore.edit {
            it[Keys.HIDDEN_REMOTE_IDS] = (it[Keys.HIDDEN_REMOTE_IDS] ?: emptySet()) + id.toString()
        }
    }

    /**
     * 숨긴 지문을 **전부 다시 꺼낸다**(2026-09-14).
     *
     * 🔴 **왜 필요한가** — 원본을 지우지 않고 목록에서만 감추는 것인데
     * **되돌릴 방법이 앱 안에 없어서 사용자에게는 사실상 삭제**였다.
     * 게다가 당시 버튼 이름이 「치우기」여서 «버린다»로 읽혔다 — 이름도 동작도 어긋나 있었다.
     * → 되돌리는 길을 내고, 이름을 **「숨기기」**로 바꿨다(동작 그대로의 이름이다).
     */
    suspend fun restoreHiddenRemotePassages() {
        context.readingDataStore.edit { it[Keys.HIDDEN_REMOTE_IDS] = emptySet() }
    }

    companion object {
        /** 퀴즈와 같은 저장소에서 받는다(§2 — 감시 대상을 늘리지 않는다). */
        private const val REMOTE_BASE_URL =
            "https://raw.githubusercontent.com/kitwlsh/korean_quiz_data/refs/heads/main/"
    }
}
