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
                modelName = "gemini-2.5-flash",
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

        val response = generativeModel?.generateContent(prompt)
        response?.text ?: "요약을 생성할 수 없습니다."
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

        val response = generativeModel?.generateContent(prompt)
        response?.text ?: ""
    }
    
    /**
     * 사용자의 맞춤형 뉴스 브리핑 명령을 처리합니다.
     */
    suspend fun processAiCustomBriefing(command: String, referenceNews: List<NewsItem>): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext "API 키가 설정되지 않았습니다."
        }

        val prompt = content {
            text("당신은 개인 비서입니다. 사용자의 다음 요청 사항에 맞춰 최신 뉴스를 분석하고 브리핑해 주세요.\n\n" +
                 "사용자 요청: \"$command\"\n\n" +
                 "참고할 뉴스 목록:\n" +
                 referenceNews.joinToString("\n") { "- ${it.title}: ${it.description}" } +
                 "\n\n위 뉴스들을 바탕으로 사용자 요청에 가장 부합하는 내용을 정리해 주세요. " +
                 "친절한 대화체로 작성해 주시고, 너무 길지 않게 핵심 위주로 요약해 주세요.")
        }

        val response = generativeModel?.generateContent(prompt)
        response?.text ?: "응답을 생성할 수 없습니다."
    }

    /**
     * 특정 텍스트나 주제를 바탕으로 AI 퀴즈를 생성합니다.
     * @param topic 주제 또는 본문 텍스트
     * @param count 생성할 문제 수
     */
    suspend fun generateQuizFromText(topic: String, count: Int = 5): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext ""
        }

        val prompt = content {
            text("당신은 전문 교육용 퀴즈 출제 위원입니다. 다음 주제나 텍스트를 바탕으로 객관식 및 주관식 퀴즈를 ${count}개 생성해 주세요.\n\n" +
                 "주제/텍스트: \"$topic\"\n\n" +
                 "제약 사항:\n" +
                 "1. 반드시 다음 JSON 형식의 배열로만 응답하세요. 다른 설명은 포함하지 마세요.\n" +
                 "2. JSON 구조: [\n" +
                 "  {\n" +
                 "    \"id\": 숫자,\n" +
                 "    \"type\": \"MULTIPLE_CHOICE\" 또는 \"SUBJECTIVE\",\n" +
                 "    \"category\": \"AI 자동 생성\",\n" +
                 "    \"subCategory\": \"$topic\",\n" +
                 "    \"question\": \"문제 내용\",\n" +
                 "    \"options\": [\"보기1\", \"보기2\", \"보기3\", \"보기4\"] (주관식인 경우 null),\n" +
                 "    \"answer\": \"정답\",\n" +
                 "    \"explanation\": \"상세 해설\",\n" +
                 "    \"semanticHint\": \"힌트\"\n" +
                 "  }\n" +
                 "]\n" +
                 "3. 정답은 명확해야 하며, 해설은 친절하게 작성해 주세요.")
        }

        val response = generativeModel?.generateContent(prompt)
        val result = response?.text ?: ""
        
        // JSON 부분만 추출 (마크다운 코드 블록 및 기타 텍스트 제거)
        val startIdx = result.indexOf("[")
        val endIdx = result.lastIndexOf("]") + 1
        
        if (startIdx != -1 && endIdx > startIdx) {
            result.substring(startIdx, endIdx).trim()
        } else {
            ""
        }
    }
}
