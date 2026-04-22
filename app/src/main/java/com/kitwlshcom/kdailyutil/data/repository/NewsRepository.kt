package com.kitwlshcom.kdailyutil.data.repository

import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class NewsRepository {

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
            items.take(limit).map { item ->
                val rawDescription = item.select("description").text()
                val cleanedDescription = Jsoup.parse(rawDescription).text()
                    .replace(Regex("(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .replace(Regex("www\\.[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .split("기사 전체 보기")[0]
                    .trim()

                NewsItem(
                    title = item.select("title").text().split(" - ")[0],
                    link = item.select("link").text(),
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

    suspend fun fetchFullContent(item: NewsItem): String = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(item.link)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            // 한국 주요 뉴스 사이트 본문 선택자 시도
            val selectors = listOf(
                "#articleBodyContents", // Naver
                "#harmonyContainer",    // Daum
                ".article_body",        // General
                ".news_article",
                "article",
                ".view_con",            // Some newspapers
                "#articleBody"
            )

            var content = ""
            for (selector in selectors) {
                val element = doc.select(selector).firstOrNull()
                if (element != null) {
                    // 불필요한 요소 제거 (스크립트, 광고 등)
                    element.select("script, style, iframe, .ad, .sns").remove()
                    content = element.text()
                    if (content.length > 100) break
                }
            }

            // 선택자로 못 찾은 경우 본문에서 가장 긴 텍스트를 가진 div 시도
            if (content.length < 100) {
                val body = doc.body()
                val divs = body.select("div")
                val longestDiv = divs.maxByOrNull { it.text().length }
                content = longestDiv?.text() ?: item.description
            }

            content.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching full content from ${item.link}", e)
            item.description // 실패 시 설명이라도 반환
        }
    }

    suspend fun getAllNews(keywords: Set<String>, limitPerKeyword: Int = 3): List<NewsItem> {
        return keywords.flatMap { keyword ->
            getNewsByKeyword(keyword, limitPerKeyword)
        }.sortedByDescending { it.pubDate } // 최신순 정렬
    }

    suspend fun getEditorials(limit: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        // 국내 주요 언론사의 사설 및 칼럼을 더 정확하게 타겟팅하기 위한 검색어 조합
        val editorialKeywords = listOf("사설", "칼럼", "시론", "오피니언")
        editorialKeywords.flatMap { keyword ->
            getNewsByKeyword(keyword, (limit / editorialKeywords.size) + 1)
        }.take(limit)
    }
}
