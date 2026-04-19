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
        val url = "$BASE_URL/search?q=$keyword&$REGION_PARAMS"
        
        try {
            val doc = Jsoup.connect(url)
                .timeout(10000)
                .parser(Parser.xmlParser())
                .get()

            val items = doc.select("item")
            items.take(limit).map { item ->
                val rawDescription = item.select("description").text()
                // 1. HTML 태그 제거
                // 2. URL 패턴 (http/https/www...) 제거
                val cleanedDescription = Jsoup.parse(rawDescription).text()
                    .replace(Regex("(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .replace(Regex("www\\.[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"), "")
                    .split("기사 전체 보기")[0] // 불필요한 꼬리표 문구 제거
                    .trim()

                NewsItem(
                    title = item.select("title").text().split(" - ")[0], // 언론사 이름 분리
                    link = item.select("link").text(),
                    description = cleanedDescription,
                    pubDate = item.select("pubDate").text(),
                    source = item.select("source").text()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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
