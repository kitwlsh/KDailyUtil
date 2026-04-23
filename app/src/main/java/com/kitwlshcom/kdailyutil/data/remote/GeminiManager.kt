package com.kitwlshcom.kdailyutil.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.kitwlshcom.kdailyutil.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiManager(private val apiKey: String?) {

    private val generativeModel by lazy {
        apiKey?.let {
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = it
            )
        }
    }

    suspend fun summarizeNews(newsItems: List<NewsItem>): String = withContext(Dispatchers.IO) {
        if (newsItems.isEmpty()) {
            return@withContext "브리핑할 뉴스가 없습니다. 키워드를 확인해 주세요."
        }

        if (generativeModel == null || apiKey.isNullOrBlank()) {
            val mockSummary = StringBuilder("현재 API 키가 설정되지 않아 데모 모드로 브리핑을 진행합니다.\n\n")
            mockSummary.append("오늘의 주요 뉴스 제목입니다.\n")
            newsItems.forEachIndexed { index, item ->
                mockSummary.append("${index + 1}번 뉴스, ${item.title}입니다.\n")
            }
            mockSummary.append("\n이상으로 데모 브리핑을 마칩니다. 실제 AI 요약을 확인하시려면 설정에서 API 키를 입력해 주세요.")
            return@withContext mockSummary.toString()
        }

        val prompt = content {
            text("다음은 최신 뉴스 목록입니다. 각 뉴스들을 분석하여 핵심 내용을 3-4문장의 자연스러운 대화체로 요약해 주세요. " +
                 "출근길에 음성으로 듣기 좋은 친절한 말투로 작성해 주세요.\n\n" +
                 newsItems.joinToString("\n") { "- ${it.title}: ${it.description}" })
        }

        try {
            val response = generativeModel?.generateContent(prompt)
            response?.text ?: "요약을 생성할 수 없습니다."
        } catch (e: Exception) {
            e.printStackTrace()
            "요약 중 오류가 발생했습니다: ${e.message}"
        }
    }

    /**
     * 뉴스 웹페이지의 HTML에서 본문 텍스트만 추출합니다.
     */
    suspend fun extractArticleContent(htmlSnippet: String): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext ""
        }

        val prompt = content {
            text("다음은 뉴스 웹페이지의 일부 텍스트 데이터입니다. 여기서 광고, 댓글, 다른 뉴스 목록, 메뉴 등을 모두 제외하고, " +
                 "오직 해당 뉴스의 '본문 기사 내용'만 추출해서 텍스트로 반환해 주세요. " +
                 "불필요한 인사말이나 서론 없이 기사 내용만 보여주세요.\n\n" +
                 "데이터:\n$htmlSnippet")
        }

        try {
            val response = generativeModel?.generateContent(prompt)
            response?.text ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
