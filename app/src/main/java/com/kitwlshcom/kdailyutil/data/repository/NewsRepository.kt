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
            items.take(limit).mapNotNull { item ->
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

            // "광고" 텍스트를 포함하는 요소 추가 제거
            doc.select("div, span, p").filter { 
                it.ownText().trim() == "광고" || it.text().trim().contains("광고주") 
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
                val webView = android.webkit.WebView(safeContext)
                activeWebViews.add(webView) // GC 방지

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
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
    suspend fun fetchFullContent(item: NewsItem): String = withContext(Dispatchers.IO) {
        val googleUrl = item.link ?: return@withContext item.description ?: ""
        
        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "📰 Processing Article: ${item.title}")

        // 1. URL 리다이렉트 해결 (Google News URL -> 실제 기사 URL)
        val finalUrl = resolveRedirect(googleUrl)
        Log.i(TAG, "🎯 FINAL TARGET URL: $finalUrl")

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
            
            extractFromBody(doc)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Tier 1 (Jsoup) failed for content: ${e.message}")
            null
        }

        if (jsoupResult != null) {
            return@withContext jsoupResult.first.also { item.fullContentHtml = jsoupResult.second }
        }

        // Tier 2: Jsoup 실패 시 WebView로 렌더링 후 시도
        Log.d(TAG, "🌐 Tier 2: Falling back to WebView for content extraction")
        val webViewHtml = fetchHtmlWithWebView(finalUrl)
        if (webViewHtml != null) {
            val doc = Jsoup.parse(webViewHtml, finalUrl)
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
                val webView = WebView(safeContext)
                activeWebViews.add(webView) // GC 방지

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/122.0.0.0"
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
        }.sortedByDescending { it.pubDate }
    }

    suspend fun getEditorials(limit: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        val editorialKeywords = listOf("사설", "칼럼", "시론", "오피니언")
        editorialKeywords.flatMap { keyword ->
            getNewsByKeyword(keyword, (limit / editorialKeywords.size) + 1)
        }.take(limit)
    }
    
}
