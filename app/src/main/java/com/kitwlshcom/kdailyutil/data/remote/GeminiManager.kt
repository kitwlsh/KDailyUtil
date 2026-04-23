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

    /**
     * 사용자의 특정 명령(커스텀 브리핑)을 처리합니다.
     * @param command 사용자의 요청 (예: "나스닥 상황을 보고 코스닥 전망해줘")
     * @param referenceNews 분석에 참고할 최신 뉴스 목록
     */
    suspend fun processAiCustomBriefing(command: String, referenceNews: List<NewsItem>): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext "AI 분석을 위해 Gemini API 키가 필요합니다."
        }

        val prompt = content {
            text("당신은 전문 뉴스 분석가이자 일상 비서입니다. 사용자의 다음 요청에 대해 수집된 뉴스 데이터를 바탕으로 심도 있는 브리핑을 작성해 주세요.\n\n" +
                 "사용자 요청: \"$command\"\n\n" +
                 "참고 뉴스 데이터:\n" +
                 referenceNews.joinToString("\n") { "- ${it.title}: ${it.description}" } + "\n\n" +
                 "요청 사항:\n" +
                 "1. 제공된 뉴스 데이터를 최대한 활용하여 요청에 답변하세요.\n" +
                 "2. 질문이 구체적이라면(예: 전망, 비교) 가능한 한 논리적인 분석을 포함하세요.\n" +
                 "3. 말투는 친절하고 전문적인 대화체로 작성해 주세요.\n" +
                 "4. 결과가 너무 길지 않게(5-7문장 내외) 핵심 위주로 작성해 주세요.")
        }

        try {
            val response = generativeModel?.generateContent(prompt)
            response?.text ?: "분석 결과를 생성할 수 없습니다."
        } catch (e: Exception) {
            e.printStackTrace()
            "분석 중 오류 발생: ${e.message}"
        }
    }
}
