package com.kitwlshcom.kdailyutil.data.disclosure

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** 알림 기록 한 줄(증시 탭 «🔔 공시 알림 기록»에서 다시 본다). */
data class AlertHistoryEntry(
    val item: DisclosureItem,
    val category: DisclosureCategory,
    val notifiedAtMillis: Long
)

/** 마지막 확인 결과 — 설정 화면이 «마지막 확인: 16:31 · 정상»처럼 말한다. */
enum class AlertCheckResult(val label: String) {
    OK("정상"),
    PRIMED("시작 준비 완료(지난 공시는 알리지 않아요)"),
    NO_CORPS("관심종목에 국내 상장사가 없어요"),
    QUOTA("공용 DART 키 한도 초과 — 오늘은 쉽니다. 개인 키를 넣으면 계속 받을 수 있어요"),
    BAD_KEY("DART 키가 올바르지 않아요"),
    NETWORK("네트워크 오류 — 다음 시각에 다시 확인합니다")
}

/**
 * 공시 알림의 **기기 안 기록** — «이미 본 공시» · 알림 기록 · 마지막 확인.
 *
 * DataStore가 아니라 SharedPreferences다: 워커(백그라운드)와 화면이 함께 읽고 쓰는데, 값이 작고
 * 갱신 단위가 «한 회차 통째»라 JSON 한 덩이로 충분하다. 동시 실행은 워커 쪽 Mutex가 막는다.
 */
class DisclosureAlertStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("disclosure_alert", Context.MODE_PRIVATE)

    // ── 이미 본 공시 (접수번호 → 본 날의 epochDay) ─────────────────────

    fun seen(): Map<String, Long> {
        val o = runCatching { JSONObject(prefs.getString(K_SEEN, "{}")!!) }.getOrNull() ?: return emptyMap()
        return o.keys().asSequence().associateWith { o.optLong(it) }
    }

    /** [add]를 더하고 [DisclosureAlertPolicy.KEEP_DAYS]보다 오래된 것은 버린다. */
    fun addSeen(add: Collection<String>, today: LocalDate) {
        val cut = today.toEpochDay() - DisclosureAlertPolicy.KEEP_DAYS
        val merged = seen().filterValues { it >= cut }.toMutableMap()
        add.forEach { merged[it] = today.toEpochDay() }
        prefs.edit().putString(K_SEEN, JSONObject(merged as Map<*, *>).toString()).apply()
    }

    // ── 첫 조회를 마친 회사 (이 회사의 지난 공시는 이미 seen에 들어 있다) ─────

    /**
     * 🔴 **켜는 순간 지난 공시가 쏟아지지 않게** — 회사를 처음 볼 때는 알리지 않고 seen만 채운다.
     * 나중에 관심종목을 새로 더해도 같은 규칙이 그 회사에 적용된다.
     */
    fun primedCorps(): Set<String> = prefs.getStringSet(K_PRIMED, emptySet())!!.toSet()

    fun setPrimedCorps(codes: Set<String>) {
        prefs.edit().putStringSet(K_PRIMED, codes).apply()
    }

    // ── 알림 기록 ─────────────────────────────────────────────────

    fun history(): List<AlertHistoryEntry> {
        val arr = runCatching { JSONArray(prefs.getString(K_HISTORY, "[]")!!) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            AlertHistoryEntry(
                item = DisclosureItem(
                    rceptNo = o.optString("r"),
                    corpCode = o.optString("c"),
                    corpName = o.optString("n"),
                    reportNm = o.optString("t"),
                    rceptDt = o.optString("d")
                ),
                category = DisclosureCategory.fromName(o.optString("k")) ?: DisclosureCategory.OTHER,
                notifiedAtMillis = o.optLong("at")
            )
        }
    }

    /** 새 기록을 **앞에** 붙이고, 30일 지난 것과 200건 넘는 것은 버린다. */
    fun addHistory(entries: List<AlertEntry>, nowMillis: Long) {
        val cut = nowMillis - DisclosureAlertPolicy.KEEP_DAYS * 24 * 60 * 60 * 1000
        val all = entries.map { AlertHistoryEntry(it.item, it.category, nowMillis) } +
            history().filter { it.notifiedAtMillis >= cut }
        val arr = JSONArray()
        all.distinctBy { it.item.rceptNo }.take(MAX_HISTORY).forEach { h ->
            arr.put(
                JSONObject()
                    .put("r", h.item.rceptNo).put("c", h.item.corpCode).put("n", h.item.corpName)
                    .put("t", h.item.reportNm).put("d", h.item.rceptDt)
                    .put("k", h.category.name).put("at", h.notifiedAtMillis)
            )
        }
        prefs.edit().putString(K_HISTORY, arr.toString()).apply()
    }

    // ── 🤖 3줄 해설 캐시 (접수번호 → 해설) — 같은 공시는 다시 부르지 않는다(§5) ─────

    fun explanation(rceptNo: String): String? =
        runCatching { JSONObject(prefs.getString(K_EXPLAIN, "{}")!!).optString(rceptNo) }
            .getOrNull()?.takeIf { it.isNotBlank() }

    /** 기록에 남아 있는 공시의 해설만 들고 간다(기록이 30일·200건으로 잘리면 해설도 같이 빠진다). */
    fun saveExplanation(rceptNo: String, text: String) {
        val o = runCatching { JSONObject(prefs.getString(K_EXPLAIN, "{}")!!) }.getOrNull() ?: JSONObject()
        o.put(rceptNo, text)
        val keep = history().map { it.item.rceptNo }.toSet() + rceptNo
        o.keys().asSequence().toList().filter { it !in keep }.forEach { o.remove(it) }
        prefs.edit().putString(K_EXPLAIN, o.toString()).apply()
    }

    // ── 마지막 확인 ───────────────────────────────────────────────

    fun lastCheck(): LocalDateTime? =
        prefs.getString(K_LAST_CHECK, null)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    fun lastResult(): AlertCheckResult? =
        prefs.getString(K_LAST_RESULT, null)?.let { n -> AlertCheckResult.entries.firstOrNull { it.name == n } }

    fun setChecked(at: LocalDateTime, result: AlertCheckResult) {
        prefs.edit().putString(K_LAST_CHECK, at.toString()).putString(K_LAST_RESULT, result.name).apply()
    }

    /** 한도에 걸린 날 — 그날 남은 회차는 조용히 쉰다(§7 B안). */
    fun quotaPausedDay(): Long = prefs.getLong(K_QUOTA_DAY, Long.MIN_VALUE)

    fun setQuotaPaused(day: LocalDate) {
        prefs.edit().putLong(K_QUOTA_DAY, day.toEpochDay()).apply()
    }

    /** 알림을 끌 때 — 다시 켜면 처음처럼 «지난 공시는 알리지 않는다»부터 시작한다. 기록은 남긴다. */
    fun resetForReenable() {
        prefs.edit().remove(K_PRIMED).remove(K_LAST_CHECK).remove(K_LAST_RESULT).remove(K_QUOTA_DAY).apply()
    }

    private companion object {
        const val K_SEEN = "seen"
        const val K_PRIMED = "primed_corps"
        const val K_HISTORY = "history"
        const val K_LAST_CHECK = "last_check"
        const val K_LAST_RESULT = "last_result"
        const val K_QUOTA_DAY = "quota_paused_day"
        const val K_EXPLAIN = "explanations"
        const val MAX_HISTORY = 200
    }
}
