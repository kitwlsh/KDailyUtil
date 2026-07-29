package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.BuildConfig
import com.kitwlshcom.kdailyutil.R
import com.kitwlshcom.kdailyutil.data.model.FamilyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * K-시리즈 자매앱 **동적 레지스트리** 로더. (2026-07-29, 표준 = doc/KLOTTO_CONNECT_HANDOFF.md §8)
 *
 * 자매앱 목록을 앱에 하드코딩하지 않고 원격 `family.json`에서 받아 렌더하므로,
 * **신규 자매앱 추가 = JSON 한 줄 편집 → 전 앱 즉시 반영(재빌드·재배포 없음)**.
 *
 * 폴백 순서(§8-4): 신선한 캐시 → 원격 → last-good 캐시(오래돼도) → 번들 기본값(`res/raw/family.json`).
 * `korean_quiz_data`를 raw로 받는 [QuizRepository] 패턴과 동일하다.
 *
 * ⚠️ 유일한 컴파일타임 제약은 `<queries>`(§8-5): 매니페스트에 없는 패키지는 설치 감지·직접 실행이
 * 불가하므로 **스토어 이동만** 동작한다(기능적으로 충분). 예약 패키지를 미리 선언해 두는 것으로 완화.
 */
object FamilyRepository {

    private const val TAG = "FamilyRepository"

    /** 원격 레지스트리(§8-2). **자기 소유 레포만** 사용한다. */
    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/kitwlsh/k-series-config/main/family.json"

    /** last-good 캐시 파일 */
    private const val CACHE_FILE = "family_config.json"

    /** 이 시간 안에는 캐시를 그대로 쓴다(앱 열 때마다 네트워크 때리지 않도록). */
    private const val REFRESH_INTERVAL_MS = 6L * 60 * 60 * 1000

    /** 레지스트리 오염/실수 대비 방어적 상한 */
    private const val MAX_APPS = 20

    /** 아는 스키마 버전. 더 높은 버전이 와도 아는 필드만 읽고 동작은 유지(전방 호환). */
    private const val SUPPORTED_SCHEMA_VERSION = 1

    private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    private val STORE_URL_PREFIXES = listOf("https://play.google.com/", "market://")
    private val ICON_HOST_SUFFIXES = listOf("githubusercontent.com", "github.io")

    /**
     * 자기 자신을 제외한 자매앱 목록을 반환한다(비활성 항목 제외, `order` 정렬).
     * @param forceRefresh true면 캐시 신선도를 무시하고 원격을 먼저 시도(설정 화면 새로고침 버튼용).
     */
    suspend fun loadSisterApps(context: Context, forceRefresh: Boolean = false): List<FamilyApp> =
        withContext(Dispatchers.IO) {
            val cache = File(context.filesDir, CACHE_FILE)
            val cacheFresh = cache.exists() &&
                System.currentTimeMillis() - cache.lastModified() < REFRESH_INTERVAL_MS

            // 1) 신선한 캐시가 있으면 네트워크 없이 즉시 사용
            if (!forceRefresh && cacheFresh) {
                parseRegistry(readOrNull(cache))?.let {
                    Log.d(TAG, "캐시 사용(신선): ${it.size}개")
                    return@withContext it
                }
            }

            // 2) 원격 로드 — 성공하면 last-good 캐시로 저장
            val remote = fetchRemote()
            if (remote != null) {
                parseRegistry(remote)?.let { apps ->
                    runCatching { cache.writeText(remote) }
                        .onFailure { Log.e(TAG, "캐시 저장 실패: ${it.message}") }
                    Log.d(TAG, "✅ 원격 레지스트리 로드: ${apps.size}개")
                    return@withContext apps
                }
            }

            // 3) 원격 실패 → last-good 캐시(오래돼도 무방)
            if (cache.exists()) {
                parseRegistry(readOrNull(cache))?.let {
                    Log.w(TAG, "⚠️ 원격 실패 — last-good 캐시 사용: ${it.size}개")
                    return@withContext it
                }
            }

            // 4) 캐시도 없음(첫 실행 오프라인 등) → 번들 기본값
            val bundled = parseRegistry(readBundled(context))
            Log.w(TAG, "⚠️ 원격·캐시 모두 불가 — 번들 기본값 사용: ${bundled?.size ?: 0}개")
            bundled ?: emptyList()
        }

    private fun readOrNull(file: File): String? =
        runCatching { file.readText() }.getOrNull()

    private fun readBundled(context: Context): String? = runCatching {
        context.resources.openRawResource(R.raw.family).bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun fetchRemote(): String? = try {
        val conn = (URL(REMOTE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            Log.w(TAG, "원격 응답 코드 ${conn.responseCode}")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "원격 로드 실패: ${e.message}")
        null
    }

    /**
     * 레지스트리 JSON → 자매앱 목록. **못 쓸 JSON이면 null**(호출부가 다음 폴백으로 넘어가도록),
     * 정상 파싱이면 목록(자매앱이 하나도 없으면 빈 목록)을 반환한다.
     */
    private fun parseRegistry(text: String?): List<FamilyApp>? {
        if (text.isNullOrBlank()) return null
        return try {
            val root = JSONObject(text)
            val schema = root.optInt("version", 1)
            if (schema > SUPPORTED_SCHEMA_VERSION) {
                Log.w(TAG, "레지스트리 스키마 v$schema > 지원 v$SUPPORTED_SCHEMA_VERSION — 아는 필드만 읽음")
            }
            val arr = root.optJSONArray("apps") ?: return null
            val apps = ArrayList<FamilyApp>(arr.length())
            for (i in 0 until arr.length()) {
                // 항목 1건이 깨져도 나머지는 살린다(퀴즈 파싱 내구성과 동일 원칙)
                try {
                    val obj = arr.optJSONObject(i) ?: continue
                    parseApp(obj)?.let { apps.add(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "apps[$i] 파싱 skip: ${e.message}")
                }
            }
            apps.sortedBy { it.order }.take(MAX_APPS)
        } catch (e: Exception) {
            Log.e(TAG, "레지스트리 파싱 실패: ${e.message}")
            null
        }
    }

    /** 유효성·화이트리스트 검사 후 [FamilyApp] 생성. 자기 자신·비활성·부적격 id는 null(제외). */
    private fun parseApp(o: JSONObject): FamilyApp? {
        val id = o.optString("id").trim()
        if (!PACKAGE_REGEX.matches(id)) {
            Log.w(TAG, "부적격 id skip: '$id'")
            return null
        }
        if (id == BuildConfig.APPLICATION_ID) return null       // 자기 자신 제외(§8-4)
        if (!o.optBoolean("active", true)) return null           // 노출 off(기획 단계 등)
        return FamilyApp(
            id = id,
            name = o.optString("name").trim().ifEmpty { id.substringAfterLast('.') },
            tagline = o.optString("tagline").trim(),
            iconUrl = sanitizeIconUrl(o.optString("iconUrl")),
            storeUrl = sanitizeStoreUrl(o.optString("storeUrl")),
            comingSoon = o.optBoolean("comingSoon", false),
            order = o.optInt("order", 0)
        )
    }

    /** 스토어 URL은 Play 도메인 / market 스킴만 허용(§8-7). 그 외는 버리고 id 기반 폴백을 쓴다. */
    private fun sanitizeStoreUrl(raw: String?): String? {
        val u = raw?.trim().orEmpty()
        if (u.isEmpty()) return null
        if (STORE_URL_PREFIXES.any { u.startsWith(it) }) return u
        Log.w(TAG, "허용되지 않은 storeUrl 무시: $u")
        return null
    }

    /** 아이콘은 https + 자기 소유 호스트(githubusercontent / github.io)만 허용(§8-7). */
    private fun sanitizeIconUrl(raw: String?): String? {
        val u = raw?.trim().orEmpty()
        if (u.isEmpty()) return null
        val host = runCatching { URL(u) }.getOrNull()
            ?.takeIf { it.protocol == "https" }?.host
        if (host != null && ICON_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }) return u
        Log.w(TAG, "허용되지 않은 iconUrl 무시: $u")
        return null
    }
}
