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
    suspend fun generateQuizFromText(topic: String, count: Int = 5): String = withContext(Dispatchers.IO)
    {
        if (generativeModel == null || apiKey.isNullOrBlank())
        {
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
                 "3. 정답은 명확해야 하며, 해설은 친절하게 작성해 주세요. 특히 'SUBJECTIVE'(주관식) 문제의 경우 정답이 긴 문장(서술형)이 아닌, 1~3단어 이내의 명사, 인명, 지명, 단어, 혹은 명확한 수치로만 출제되도록 하세요. 긴 문장을 그대로 입력해야 정답 처리되는 주관식 문제는 출제하지 마세요.")
        }

        val response = generativeModel?.generateContent(prompt)
        val result = response?.text ?: ""
        
        return@withContext cleanJsonString(result)
    }

    /**
     * 마크다운 펜스 및 찌꺼기 문자들을 걸러내어 완전한 JSON 서브스트링만 파싱합니다.
     */
    private fun cleanJsonString(text: String): String
    {
        var cleaned = text.trim()
        
        if (cleaned.startsWith("```"))
        {
            val nextNewLine = cleaned.indexOf("\n")
            if (nextNewLine != -1)
            {
                cleaned = cleaned.substring(nextNewLine).trim()
            }
        }
        
        if (cleaned.endsWith("```"))
        {
            cleaned = cleaned.substring(0, cleaned.length - 3).trim()
        }

        val startIdx = cleaned.indexOf("[")
        val endIdx = cleaned.lastIndexOf("]") + 1
        if (startIdx != -1 && endIdx > startIdx)
        {
            return cleaned.substring(startIdx, endIdx).trim()
        }

        val startObjIdx = cleaned.indexOf("{")
        val endObjIdx = cleaned.lastIndexOf("}") + 1
        if (startObjIdx != -1 && endObjIdx > startObjIdx)
        {
            return cleaned.substring(startObjIdx, endObjIdx).trim()
        }

        return cleaned
    }

    /**
     * 다중 이미지 스캔을 통해 지능형으로 맥락을 융합하고 중복 없는 취약점 표적 퀴즈를 생성합니다.
     */
    suspend fun generateQuizzesFromImages(
        images: List<android.graphics.Bitmap>,
        previousQuizzesJson: String,
        errorStatsJson: String,
        count: Int = 5
    ): String = withContext(Dispatchers.IO)
    {
        if (generativeModel == null || apiKey.isNullOrBlank())
        {
            return@withContext ""
        }

        val prompt = content {
            images.forEach { bmp ->
                image(bmp)
            }
            
            text(
                "당신은 전문 교육용 퀴즈 출제 위원입니다. 제공된 이미지(교과서, 문제집 등)들을 정밀 스캔하여 고도의 완성도를 지닌 퀴즈를 ${count}개 생성해 주세요.\n\n" +
                "이전 출제되었던 문제 목록(중복 방지용):\n$previousQuizzesJson\n\n" +
                "사용자의 학습 분석 오답 기록(취약 영역 저격용):\n$errorStatsJson\n\n" +
                "제약 사항 및 요구 조건:\n" +
                "1. **저작권 보호 및 재창작(Paraphrasing) 의무**:\n" +
                "   - 이미지 안의 본문 텍스트를 절대로 그대로 타이핑하여 복제(Verbatim Copy)하지 마세요.\n" +
                "   - 반드시 이미지 안의 지식과 개념을 파악하여 질문과 보기, 해설을 **독창적으로 요약 및 변형 가공**하여 새로운 문제로 재창작해 주세요.\n" +
                "2. **맥락 기반 중복 방지**:\n" +
                "   - 제공된 '이전 출제되었던 문제 목록'에 있는 질문들과 유사하거나 중복되는 문제가 출제되지 않도록 완전히 새로운 문제를 설계하세요.\n" +
                "3. **취약 단원 타겟팅**:\n" +
                "   - 제공된 '오답 기록' 정보가 있다면, 사용자가 자주 틀렸던 주제와 개념에 관련된 복습/함정 질문을 1-2개 유도하여 성취도를 평가하세요.\n" +
                "4. **완벽한 JSON 배열 형식 응답**:\n" +
                "   - 반드시 다음 JSON 형식의 배열로만 응답하세요. 다른 본문 텍스트나 설명은 제외하세요.\n" +
                "   - JSON 구조:\n" +
                "     [\n" +
                "       {\n" +
                "         \"id\": 숫자,\n" +
                "         \"type\": \"MULTIPLE_CHOICE\" 또는 \"SUBJECTIVE\",\n" +
                "         \"category\": \"AI 스캔 퀴즈\",\n" +
                "         \"subCategory\": \"이미지 분석 핵심 단원\",\n" +
                "         \"question\": \"재창작된 질문 내용\",\n" +
                "         \"options\": [\"보기1\", \"보기2\", \"보기3\", \"보기4\"] (주관식인 경우 null),\n" +
                "         \"answer\": \"정답\",\n" +
                "         \"explanation\": \"상세한 오답 해설 및 개념 설명\",\n" +
                "         \"semanticHint\": \"힌트\"\n" +
                "       }\n" +
                "     ]\n" +
                "5. 정답은 명확하고 반론의 여지가 없어야 하며, 오답 보기들도 상당히 설득력 있고 매끄럽게 구성되어야 합니다. 특히 'SUBJECTIVE'(주관식) 문제의 경우 정답이 긴 문장(서술형)이 아닌, 1~3단어 이내의 명사, 인명, 지명, 단어, 혹은 명확한 수치로만 출제되도록 하세요. 긴 문장을 그대로 입력해야 정답 처리되는 주관식 문제는 출제하지 마세요."
            )
        }

        return@withContext try
        {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text ?: ""
            cleanJsonString(result)
        }
        catch (e: Exception)
        {
            android.util.Log.e("GeminiManager", "❌ generateQuizzesFromImages error: ${e.message}", e)
            ""
        }
    }

    /**
     * 질문과 정답만을 사용하여 그럴싸한 오답 3개와 해설을 포함한 JSON을 생성합니다.
     */
    suspend fun generateOptionsForQuestion(
        question: String,
        answer: String
    ): String = withContext(Dispatchers.IO)
    {
        if (generativeModel == null || apiKey.isNullOrBlank())
        {
            return@withContext ""
        }

        val prompt = content {
            text(
                "당신은 전문 교육용 퀴즈 출제 보조 장치입니다. 주어진 질문과 정답을 기반으로, 퀴즈를 사지선다(객관식)로 구성할 수 있게 3개의 매끄럽고 설득력 있는 '오답(틀린 보기)'과 퀴즈에 대한 상세한 '해설'을 자동 생성해 주세요.\n\n" +
                "질문: \"$question\"\n" +
                "정답: \"$answer\"\n\n" +
                "요구 조건:\n" +
                "1. 정답을 뺀 오답 보기 3개는 정답인 것처럼 그럴싸하고 혼동하기 쉬워야 합니다. 전혀 터무니없는 보기는 배제해 주세요.\n" +
                "2. 반드시 다음 JSON 객체 형식으로만 응답하세요. 다른 부가적인 텍스트나 설명은 포함하지 마세요.\n" +
                "   JSON 구조:\n" +
                "   {\n" +
                "     \"options\": [\"정답내용\", \"오답1\", \"오답2\", \"오답3\"] (4개의 보기가 셔플되지 않은 채 정답을 포함한 배열로 되어야 합니다. 정답은 무조건 첫 번째 요소로 넣어주세요),\n" +
                "     \"explanation\": \"정답이 왜 정답이고 오답들이 왜 틀렸는지 설명해 주는 아주 명쾌하고 친절한 해설 내용\"\n" +
                "   }\n" +
                "3. 반드시 JSON 형식 문자열만 반환하세요."
            )
        }

        return@withContext try
        {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text ?: ""
            cleanJsonString(result)
        }
        catch (e: Exception)
        {
            android.util.Log.e("GeminiManager", "❌ generateOptionsForQuestion error: ${e.message}", e)
            ""
        }
    }

    /**
     * 주관식 답변이 정답과 일치하거나 의미상 동등한지 검증합니다.
     */
    suspend fun verifySubjectiveAnswer(
        question: String,
        correctAnswer: String,
        userAnswer: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext false
        }

        val prompt = content {
            text(
                "당신은 퀴즈 정답 채점 위원입니다. 다음 질문에 대한 제시된 정답과 사용자의 입력 정답을 비교하여, 사용자의 정답이 맞는지 판정해 주세요.\n\n" +
                "질문: \"$question\"\n" +
                "제시된 정답: \"$correctAnswer\"\n" +
                "사용자 입력: \"$userAnswer\"\n\n" +
                "채점 기준:\n" +
                "1. 동의어/유의어: 의미상 또는 문맥상 동일한 대상이나 개념을 가리키는 경우 정답(true)으로 판정합니다.\n" +
                "2. 영어/한글 발음: 영어 정답을 한글 발음으로 입력했거나(예: 'Washington'에 대해 '워싱턴', '워싱턴 DC'에 대해 '워싱턴' 또는 '워싱턴디씨' 또는 '워싱턴DC' 등) 혹은 그 반대의 경우에도 정답(true)으로 판정합니다.\n" +
                "3. 핵심 단어 포함: 약어, 부분 단어(예: '대한민국'에 대해 '한국')가 문맥상 정답으로 충분히 유동적으로 해석 가능하면 정답(true)으로 판정합니다.\n" +
                "4. 주관식/서술형: 정답이 비교적 긴 문장인 경우, 핵심 단어가 포함되어 있고 문장의 의미가 제시된 정답과 문맥상 일치하면 정답(true)으로 판정합니다.\n" +
                "5. 반드시 다음 JSON 형식으로만 응답하세요. 다른 본문 텍스트나 설명은 제외하세요.\n" +
                "   JSON 구조: { \"isCorrect\": true 또는 false }"
            )
        }

        return@withContext try {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text ?: ""
            val cleaned = cleanJsonString(result)
            val obj = org.json.JSONObject(cleaned)
            obj.getBoolean("isCorrect")
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "❌ verifySubjectiveAnswer error: ${e.message}", e)
            false
        }
    }
}
