package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume

class NewsRepository(private val context: Context? = null) {

    companion object {
        private const val TAG = "NewsRepository"
        private const val BASE_URL = "https://news.google.com/rss"
        private const val REGION_PARAMS = "hl=ko&gl=KR&ceid=KR:ko"

        /**
         * AI 이용을 명시적으로 금지한 것으로 알려진 매체 도메인 (본문 고지 감지를 못하는 경우의 백스톱).
         * 필요 시 여기에 도메인을 추가하면 본문 문구가 안 잡혀도 차단됩니다.
         */
        private val AI_RESTRICTED_DOMAINS = listOf(
            "imnews.imbc.com", "imbc.com", "mbc.co.kr",  // MBC
            "industrynews.co.kr",                          // 인더스트리뉴스 (AI학습 및 활용 금지)
            "kmib.co.kr"                                   // 국민일보 (AI학습 이용 금지)
        )

        /**
         * 본문/페이지 텍스트에서 'AI 학습·이용 금지' 류 저작권 고지를 감지한다.
         * (일반적인 '무단 전재·재배포 금지'는 거의 모든 기사에 있으므로 차단 대상에서 제외하고,
         *  AI 관련 명시적 옵트아웃만 차단 신호로 사용한다.)
         */
        fun detectAiRestrictionNotice(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            // 공백 제거 정규화 ('AI 학습' / 'AI학습' 모두 매칭)
            val norm = text.replace("\\s".toRegex(), "")
            val aiTerms = listOf(
                "AI학습", "에이아이학습", "인공지능학습", "기계학습", "머신러닝", "딥러닝",
                "AI이용", "AI활용", "AI학습포함", "인공지능이용", "인공지능활용",
                "데이터마이닝", "텍스트마이닝"
            )
            val hasAiTerm = aiTerms.any { norm.contains(it, ignoreCase = true) }
            if (!hasAiTerm) return false
            // 금지/불가 등 제한 의도가 함께 있을 때만 (AI를 다룬 일반 기사 오탐 방지)
            val prohibitTerms = listOf("금지", "불가", "할수없", "허용하지", "동의없이", "무단")
            return prohibitTerms.any { norm.contains(it) }
        }

        fun isAiRestrictedDomain(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val lower = url.lowercase()
            return AI_RESTRICTED_DOMAINS.any { lower.contains(it) }
        }
    }

    private fun parsePubDateToMillis(pubDate: String?): Long {
        if (pubDate.isNullOrBlank()) return 0L
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                return sdf.parse(pubDate.trim())?.time ?: 0L
            } catch (e: Exception) {
                // Ignore and try next format
            }
        }
        return 0L
    }

    suspend fun getNewsByKeyword(keyword: String, limit: Int = 3): List<NewsItem> = withContext(Dispatchers.IO) {
        val url = if (keyword.isBlank()) {
            "$BASE_URL?$REGION_PARAMS"
        } else {
            "$BASE_URL/search?q=$keyword&$REGION_PARAMS"
        }
        
        try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .parser(Parser.xmlParser())
                .get()

            val items = doc.select("item")
            items.mapNotNull { item ->
                val link = item.select("link").text()
                // 조선일보 제외
                if (link.contains("chosun.com")) return@mapNotNull null

                val rawDescription = item.select("description").text()
                val cleanedDescription = Jsoup.parse(rawDescription).text()
                    .replace(Regex("(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .replace(Regex("www\\.[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .split("기사 전체 보기")[0]
                    .trim()

                NewsItem(
                    title = item.select("title").text().split(" - ")[0],
                    link = link,
                    description = cleanedDescription,
                    pubDate = item.select("pubDate").text(),
                    source = item.select("source").text()
                )
            }
            .sortedByDescending { parsePubDateToMillis(it.pubDate) }
            .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching news for keyword: $keyword", e)
            emptyList()
        }
    }

    suspend fun getTopNews(limit: Int = 10): List<NewsItem> = getNewsByKeyword("", limit)

    /**
     * 핵심 본문 및 HTML 추출 로직
     * @return Pair(Plain Text, Cleaned HTML)
     */
    private fun extractFromBody(doc: Document): Pair<String, String>? {
        try {
            // 1. 불필요한 요소 제거 (광고, 네비게이션, 스타일, 스크립트 등)
            val noiseSelectors = listOf(
                "script", "style", "noscript", "iframe", "header", "footer", "nav", "aside",
                "button", "input", "textarea", "form", "svg", "path", "video", "canvas",
                ".subtitle", ".article-subtitle", ".at_sub_ttl", // 부제목 제거
                ".author", ".byline", ".reporter", ".author_info", // 기자 정보 제거
                ".date", ".publish-date", ".time", // 날짜 정보 제거
                ".article-audio", ".audio-player", ".audio_area", // 오디오 플레이어 제거
                ".ads", "#ads", ".ad", ".banner", ".social", ".related", ".reply", ".comment",
                ".footer", ".header", ".menu", ".nav", ".top_menu", ".sidebar", ".bottom",
                ".copyright", ".meta", ".share", ".btn", ".tags", ".keyword"
            )
            doc.select(noiseSelectors.joinToString(", ")).remove()

            // "광고" 또는 "광고주" 텍스트를 *자체적으로* 포함하는 단일 요소(Leaf 노드 위주)만 제거하여 부모 컨테이너가 통째로 지워지는 문제 방지
            doc.select("div, span, p").filter { 
                val ownText = it.ownText().trim()
                ownText == "광고" || ownText.contains("광고주") 
            }.forEach { it.remove() }

            // 2. 주요 본문 셀렉터 탐색 (우선순위 기반)
            // .text 가 .article-text 보다 앞에 오도록 하여 실제 본문 영역을 먼저 잡도록 함
            val contentSelectors = listOf(
                ".text", ".article-text", "#articleBodyContents", "#dic_area", "#harmonyContainer", 
                "#news_body_area", "#news_body", ".news_body", ".article_body", ".article_content", 
                ".view_content", "article", "[itemprop=articleBody]", 
                ".article_view", "#article_view", "#article_content", ".article_txt",
                "#article_txt", ".content_area", ".post-content", ".story-content",
                ".article_view_body", ".news_view_body", ".text_area"
            )

            for (selector in contentSelectors) {
                val elements = doc.select(selector)
                if (elements.isNotEmpty()) {
                    // 이미지 절대 경로 변환
                    elements.select("img").forEach { img ->
                        val absUrl = img.absUrl("src")
                        if (absUrl.isNotBlank()) img.attr("src", absUrl)
                        // 불필요한 속성 제거
                        img.removeAttr("srcset")
                        img.removeAttr("sizes")
                        img.removeAttr("loading")
                    }

                    // 모든 매칭 요소의 텍스트와 HTML을 합침
                    val joinedText = elements.joinToString("\n\n") { it.text() }
                    val joinedHtml = elements.joinToString("<br><br>") { it.outerHtml() }
                    
                    val cleanedText = cleanFinalText(joinedText)
                    
                    if (isLikelyContent(cleanedText)) {
                        val finalHtml = sanitizeHtml(joinedHtml)
                        return Pair(
                            if (cleanedText.length > 5000) cleanedText.take(5000) else cleanedText,
                            finalHtml
                        )
                    }
                }
            }

            // 3. 셀렉터 매칭 실패 시: 가장 긴 텍스트 덩어리를 가진 태그 탐색
            var bestElement: org.jsoup.nodes.Element? = null
            var bestLength = 0
            doc.select("div, section, article").forEach { el ->
                val text = cleanFinalText(el.text())
                if (text.length > bestLength && isLikelyContent(text)) {
                    bestLength = text.length
                    bestElement = el
                }
            }
            
            bestElement?.let { el ->
                if (bestLength > 200) {
                    el.select("img").forEach { img ->
                        val absUrl = img.absUrl("src")
                        if (absUrl.isNotBlank()) img.attr("src", absUrl)
                    }
                    return Pair(cleanFinalText(el.text()), sanitizeHtml(el.outerHtml()))
                }
            }

            // 4. 최종 폴백: 전체 Body
            val body = doc.body()
            if (body != null) {
                // 폴백 시에도 최소한의 노이즈 제거 재수행 (이스케이프 해제 후 다시 파싱된 경우 대비)
                body.select("script, style, iframe, .ads, #ads").remove()
                
                val text = cleanFinalText(body.text())
                if (isLikelyContent(text) && text.length > 200) {
                    body.select("img").forEach { img ->
                        val absUrl = img.absUrl("src")
                        if (absUrl.isNotBlank()) img.attr("src", absUrl)
                    }
                    return Pair(text, sanitizeHtml(body.outerHtml()))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in extractFromBody: ${e.message}")
        }
        return null
    }

    /**
     * HTML에서 광고성 태그 등을 추가로 정리
     */
    private fun sanitizeHtml(html: String): String {
        val doc = Jsoup.parse(html)
        // 본문 내부에 남아있을 수 있는 불필요 요소 제거
        val extraNoise = listOf(".adsbygoogle", "script", "style", "iframe", ".ad-container", ".social-share")
        doc.select(extraNoise.joinToString(", ")).remove()
        
        // 이미지 태그 스타일 조정 (화면에 꽉 차게)
        doc.select("img").forEach { 
            it.attr("style", "max-width: 100%; height: auto; display: block; margin: 10px auto;") 
        }
        
        return doc.body().html()
    }

    /**
     * 텍스트가 실제 기사 내용인지 검증하는 유틸리티
     */
    private fun isLikelyContent(text: String): Boolean {
        if (text.length < 50) return false // 더 짧은 본문도 허용
        
        // CSS나 스크립트 코드 패턴 감지 (더 완화)
        val codeMarkers = listOf("@font-face", "background:", "color:", "margin:", "display:", "function(", "var ", "let ", "padding:", "border:")
        val codeCount = codeMarkers.count { text.contains(it) }
        if (codeCount > 15) { // 15개 이상일 때만 코드로 간주
            Log.d(TAG, "🚫 Likely Code (count $codeCount): ${text.take(50)}...")
            return false
        }
        
        // 한글 비중 체크 (기사의 경우 한글 비중이 어느 정도 있어야 함)
        val koreanChars = text.count { it in '\uAC00'..'\uD7A3' }
        val koreanRatio = if (text.isNotEmpty()) koreanChars.toDouble() / text.length else 0.0
        
        val isLikely = koreanRatio > 0.1 // 10%만 넘어도 허용 (기술 기사 등 고려)
        if (!isLikely) {
            Log.d(TAG, "🚫 Low Korean Ratio ($koreanRatio): ${text.take(50)}...")
        }
        return isLikely
    }



    private suspend fun resolveRedirect(url: String): String = withContext(Dispatchers.IO) {
        if (!url.contains("google.com")) {
            return@withContext url
        }
        
        // Tier 1: 고강도 바이너리 스캐너 (2025 최적화)
        if (url.contains("news.google.com/rss/articles/")) {
            try {
                decodeGoogleNewsUrl(url)?.let { decodedUrl ->
                    Log.i(TAG, "✅ [Tier 1] Decoded: $decodedUrl")
                    return@withContext decodedUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ [Tier 1] Fail: ${e.message}")
            }
        }

        // Tier 2: Jsoup Standard Redirect + Meta Refresh
        Log.d(TAG, "🔍 [Tier 2] Jsoup Resolving: $url")
        var landedUrl = try {
            val response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .followRedirects(true)
                .timeout(8000)
                .execute()
            
            var currentUrl = response.url().toString()
            
            // 만약 여전히 Google URL이라면 HTML 내부의 meta refresh 확인
            if (currentUrl.contains("google.com")) {
                val doc = response.parse()
                val metaRefresh = doc.select("meta[http-equiv=refresh]").first()
                val content = metaRefresh?.attr("content")
                if (content != null && content.contains("url=")) {
                    val nextUrl = content.substringAfter("url=").trim()
                    if (nextUrl.startsWith("http")) {
                        Log.i(TAG, "🔄 [Tier 2] Meta Refresh Detected: $nextUrl")
                        currentUrl = nextUrl
                    }
                }
            }
            currentUrl
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [Tier 2] Jsoup Fail: ${e.message}")
            url
        }

        if (!landedUrl.contains("google.com")) {
            Log.i(TAG, "✅ [Tier 2] Resolved: $landedUrl")
            return@withContext landedUrl
        }

        // Tier 3: WebView 로켓 엔진
        Log.d(TAG, "🌐 [Tier 3] WebView Fallback: $landedUrl")
        return@withContext resolveRedirectWithWebView(landedUrl) ?: landedUrl
    }

    private fun decodeGoogleNewsUrl(sourceUrl: String): String? {
        return try {
            val encodedPart = sourceUrl.substringAfter("articles/").substringBefore("?")
            
            // URL-Safe Base64 디코딩 (패딩 무시 옵션 포함)
            val bytes = android.util.Base64.decode(encodedPart, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
            
            // 바이트 배열에서 URL 패턴(http) 탐색
            val content = String(bytes, StandardCharsets.UTF_8)
            val httpIndex = content.indexOf("http")
            
            if (httpIndex != -1) {
                // 제어 문자나 인용구 전까지 URL 추출
                val rawUrl = content.substring(httpIndex).takeWhile { 
                    it.toInt() in 32..126 && it != '"' && it != '\'' && it != '<' && it != '>'
                }
                if (rawUrl.contains(".") && !rawUrl.contains("google.com")) {
                    return rawUrl
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Decoder Exception: ${e.message}")
            null
        }
    }

    // WebView 인스턴스가 GC되는 것을 방지하기 위한 임시 보관소
    private val activeWebViews = mutableListOf<android.webkit.WebView>()

    private suspend fun resolveRedirectWithWebView(url: String): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val safeContext = context ?: run {
            Log.e(TAG, "❌ WebView Context is Null")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                var isResolved = false
                val webView = try {
                    android.webkit.WebView(safeContext)
                } catch (t: Throwable) {
                    Log.e(TAG, "WebView instantiation failed: ${t.message}")
                    continuation.resume(null)
                    return@post
                }
                activeWebViews.add(webView) // GC 방지

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    // 보안: 외부 페이지의 단말 내부 파일 접근 차단
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                }

                val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!isResolved) {
                        isResolved = true
                        Log.w(TAG, "⌛ WebView Timeout: $url")
                        activeWebViews.remove(webView)
                        continuation.resume(null)
                        webView.destroy()
                    }
                }

                webView.webViewClient = object : android.webkit.WebViewClient() {
                    private fun checkUrl(currentUrl: String?): Boolean {
                        val target = currentUrl ?: return false
                        // Google URL이 아니고 실제 웹 주소로 보이면 성공
                        if (!isResolved && !target.contains("google.com") && !target.contains("about:blank") && target.startsWith("http")) {
                            isResolved = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            Log.i(TAG, "🚀 WebView Success Redirect: $target")
                            activeWebViews.remove(webView)
                            continuation.resume(target)
                            webView.destroy()
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        Log.d(TAG, "🌐 WebView Start: $url")
                        checkUrl(url)
                    }

                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        Log.d(TAG, "🌐 WebView Finished: $url")
                        checkUrl(url)
                    }

                    override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val target = request?.url?.toString()
                        return checkUrl(target)
                    }
                }
                
                timeoutHandler.postDelayed(timeoutRunnable, 12000)
                webView.loadUrl(url)
            } catch (e: Exception) {
                Log.e(TAG, "❌ WebView Error: ${e.message}")
                continuation.resume(null)
            }
        }
    }

    /**
     * 최종 기사 본문 추출 (Jsoup 실패 시 WebView로 재시도하는 2단계 전략)
     */
    /**
     * 저작권 보호 정책: 본문 전문을 스크랩/복제하지 않는다.
     * 읽기는 WebView 원문으로 제공하므로, 표시용 원본 URL만 해석하고
     * (도메인/스니펫 기반) AI 이용 제한 여부만 플래그로 설정한다.
     */
    suspend fun resolveArticleUrl(item: NewsItem): String = withContext(Dispatchers.IO) {
        val googleUrl = item.link ?: return@withContext ""
        val finalUrl = resolveRedirect(googleUrl)
        item.resolvedUrl = finalUrl
        item.aiRestricted = isAiRestrictedDomain(finalUrl) ||
            isAiRestrictedDomain(googleUrl) ||
            detectAiRestrictionNotice(item.description)
        finalUrl
    }

    /**
     * ⚠️ [사용 금지 - 저작권 정책] 기사 본문 전문을 스크랩하는 레거시 함수.
     * 무단 전재·AI 학습 금지 정책 준수를 위해 더 이상 호출하지 않는다.
     * 읽기는 WebView 원문, 브리핑/쉐도잉은 RSS 스니펫만 사용한다. (resolveArticleUrl 참고)
     * 보존 사유: 추출 로직 참고용. 새 코드에서 호출하지 말 것.
     */
    @Deprecated("저작권 정책상 본문 전문 스크랩 금지. resolveArticleUrl + 스니펫을 사용하세요.")
    suspend fun fetchFullContent(item: NewsItem): String = withContext(Dispatchers.IO) {
        val googleUrl = item.link ?: return@withContext item.description ?: ""

        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "📰 Processing Article: ${item.title}")

        // 1. URL 리다이렉트 해결 (Google News URL -> 실제 기사 URL)
        val finalUrl = resolveRedirect(googleUrl)
        item.resolvedUrl = finalUrl
        Log.i(TAG, "🎯 FINAL TARGET URL: $finalUrl")

        // 도메인 차단목록 선반영 (본문 고지 감지를 못하는 경우의 백스톱)
        item.aiRestricted = isAiRestrictedDomain(finalUrl) || detectAiRestrictionNotice(item.description ?: "")

        if (finalUrl.contains("google.com/url?") || finalUrl.contains("news.google.com/rss/articles/")) {
            Log.w(TAG, "⚠️ FAILED TO ESCAPE GOOGLE: $finalUrl")
            return@withContext item.description ?: ""
        }

        // 2단계 추출 전략 시작
        // Tier 1: Jsoup으로 가볍게 시도
        val jsoupResult = try {
            val doc = Jsoup.connect(finalUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .timeout(8000)
                .followRedirects(true)
                .get()

            // 전체 페이지 텍스트(푸터 저작권 고지 포함)에서 'AI 학습/이용 금지' 감지
            if (detectAiRestrictionNotice(doc.text())) item.aiRestricted = true
            extractFromBody(doc)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Tier 1 (Jsoup) failed for content: ${e.message}")
            null
        }

        if (jsoupResult != null) {
            if (item.aiRestricted) Log.i(TAG, "🚫 AI/낭독 제한 매체로 감지됨: ${item.title}")
            return@withContext jsoupResult.first.also { item.fullContentHtml = jsoupResult.second }
        }

        // Tier 2: Jsoup 실패 시 WebView로 렌더링 후 시도
        Log.d(TAG, "🌐 Tier 2: Falling back to WebView for content extraction")
        val webViewHtml = fetchHtmlWithWebView(finalUrl)
        if (webViewHtml != null) {
            val doc = Jsoup.parse(webViewHtml, finalUrl)
            if (detectAiRestrictionNotice(doc.text())) item.aiRestricted = true
            val webViewResult = extractFromBody(doc)
            if (webViewResult != null) {
                Log.d(TAG, "✅ Success! Content extracted via WebView")
                item.fullContentHtml = webViewResult.second
                return@withContext webViewResult.first
            }
        }

        Log.w(TAG, "❌ All content extraction strategies failed. Returning description.")
        item.fullContentHtml = item.description 
        return@withContext item.description ?: ""
    }

    /**
     * WebView를 사용하여 페이지의 HTML 소스를 긁어옴
     */
    private suspend fun fetchHtmlWithWebView(url: String): String? = suspendCancellableCoroutine<String?> { continuation ->
        val safeContext = context ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        Handler(Looper.getMainLooper()).post {
            try {
                var isFinished = false
                val webView = try {
                    WebView(safeContext)
                } catch (t: Throwable) {
                    Log.e(TAG, "WebView instantiation failed: ${t.message}")
                    continuation.resume(null)
                    return@post
                }
                activeWebViews.add(webView) // GC 방지

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/122.0.0.0"
                    // 보안: 외부 페이지의 단말 내부 파일 접근 차단
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (isFinished) return
                        Log.d(TAG, "🌐 Content WebView Finished: $url")
                        
                        // 페이지 로딩 후 JS 실행 대기 시간을 2초로 단축 (대신 타임아웃 넉넉히)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!isFinished) {
                                view?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                                    if (!isFinished) {
                                        isFinished = true
                                        val cleanHtml = html?.replace("\\\\n", "\n")
                                            ?.replace("\\\\t", "\t")
                                            ?.replace("\\\\\"", "\"")
                                            ?.replace("\\\\u003C", "<")
                                            ?.replace("\\\\u003E", ">")
                                            ?.replace("\\\\u0026", "&")
                                            ?.removePrefix("\"")?.removeSuffix("\"")
                                            ?.replace("\\\"", "\"")

                                        Log.d(TAG, "✅ HTML Extracted (size: ${cleanHtml?.length ?: 0})")
                                        activeWebViews.remove(webView)
                                        continuation.resume(cleanHtml)
                                        webView.destroy()
                                    }
                                }
                            }
                        }, 2500) 
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (!isFinished) {
                            Log.w(TAG, "⚠️ WebView Error: ${error?.description}")
                            isFinished = true
                            activeWebViews.remove(webView)
                            continuation.resume(null)
                            webView.destroy()
                        }
                    }
                }
                
                webView.loadUrl(url)

                // 타임아웃 처리 (15초)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinished) {
                        Log.w(TAG, "⌛ Content WebView Timeout")
                        isFinished = true
                        activeWebViews.remove(webView)
                        continuation.resume(null)
                        webView.destroy()
                    }
                }, 15000)

            } catch (e: Exception) {
                Log.e(TAG, "WebView initialization failed: ${e.message}")
                continuation.resume(null)
            }
        }
    }

    private fun cleanFinalText(text: String): String {
        return text
            .replace("Your browser does not support the audio element.", "")
            .replace("기사를 읽어드립니다", "")
            .replace("무단전재 및 재배포 금지", "")
            .replace("저작권자", "")
            .replace("재배포 금지", "")
            .replace("광고", "")
            // 이메일 주소 (다중 도메인 대응)
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), "")
            // [도시]/[이름] [직함] 패턴 (예: 워싱턴/김원철 특파원)
            .replace(Regex("[가-힣]{2,10}/[가-힣]{2,5}\\s?(기자|특파원|논설위원|연구원)"), "")
            // 이름 + 직함 패턴 (공백 유무 상관없이)
            .replace(Regex("[가-힣]{2,5}\\s?(기자|특파원|논설위원|연구원)"), "")
            // 문장 끝에 남는 의미 없는 기호들 정리
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[,./\\s]+$"), "") 
            .trim()
    }

    suspend fun getAllNews(keywords: Set<String>, limitPerKeyword: Int = 3): List<NewsItem> {
        return keywords.flatMap { keyword ->
            getNewsByKeyword(keyword, limitPerKeyword)
        }.sortedByDescending { parsePubDateToMillis(it.pubDate) }
    }

    private fun getCacheFile(cacheKey: String): java.io.File? {
        val safeContext = context ?: return null
        val encodedKey = android.util.Base64.encodeToString(
            cacheKey.toByteArray(java.nio.charset.StandardCharsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
        return java.io.File(safeContext.filesDir, "news_cache_$encodedKey.json")
    }

    suspend fun saveCachedNews(cacheKey: String, newsList: List<NewsItem>) = withContext(Dispatchers.IO) {
        val file = getCacheFile(cacheKey) ?: return@withContext
        try {
            val jsonArray = org.json.JSONArray()
            for (item in newsList) {
                val jsonObj = org.json.JSONObject().apply {
                    put("title", item.title)
                    put("link", item.link)
                    put("description", item.description)
                    put("pubDate", item.pubDate)
                    put("source", item.source)
                    put("summary", item.summary)
                    put("fullContent", item.fullContent)
                    put("fullContentHtml", item.fullContentHtml)
                    put("resolvedUrl", item.resolvedUrl)
                    put("aiRestricted", item.aiRestricted)
                }
                jsonArray.put(jsonObj)
            }
            file.writeText(jsonArray.toString(), java.nio.charset.StandardCharsets.UTF_8)
            Log.d(TAG, "💾 Saved $cacheKey cache to file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save news cache for $cacheKey", e)
        }
    }

    suspend fun loadCachedNews(cacheKey: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val file = getCacheFile(cacheKey) ?: return@withContext emptyList()
        if (!file.exists()) return@withContext emptyList()
        try {
            val jsonStr = file.readText(java.nio.charset.StandardCharsets.UTF_8)
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<NewsItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                list.add(
                    NewsItem(
                        title = jsonObj.optString("title", ""),
                        link = jsonObj.optString("link", ""),
                        description = jsonObj.optString("description", ""),
                        pubDate = jsonObj.optString("pubDate", ""),
                        source = jsonObj.optString("source", ""),
                        summary = jsonObj.optString("summary", ""),
                        fullContent = jsonObj.optString("fullContent", ""),
                        fullContentHtml = jsonObj.optString("fullContentHtml", ""),
                        resolvedUrl = jsonObj.optString("resolvedUrl", ""),
                        aiRestricted = jsonObj.optBoolean("aiRestricted", false)
                    )
                )
            }
            Log.d(TAG, "📂 Loaded $cacheKey cache from file (size: ${list.size})")
            list
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load news cache for $cacheKey", e)
            emptyList()
        }
    }

    fun getCacheLastModified(cacheKey: String): Long {
        val file = getCacheFile(cacheKey)
        return if (file != null && file.exists()) {
            file.lastModified()
        } else {
            0L
        }
    }
}
