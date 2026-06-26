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
     * 책 페이지 사진에서 본문 텍스트만 OCR로 추출합니다. (빠른 독서 훈련 지문용)
     */
    suspend fun extractTextFromImage(image: android.graphics.Bitmap): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) return@withContext ""
        val prompt = content {
            image(image)
            text(
                "이 이미지는 책의 펼친 지면입니다(한 쪽 또는 두 쪽, 여러 단일 수 있음). " +
                "보이는 '본문 텍스트 전체'를 빠짐없이 읽어 주세요. " +
                "두 쪽이면 왼쪽 쪽을 위에서 아래로 읽은 뒤 오른쪽 쪽을 읽고, 읽는 순서대로 문장을 자연스럽게 이어 주세요. " +
                "쪽번호·장 제목 머리말·꼬리말은 제외하고, 일반 텍스트(평문)로만 반환하세요. " +
                "설명·따옴표·마크다운 없이 본문만 출력하세요."
            )
        }
        val out = (generativeModel?.generateContent(prompt)?.text ?: "").trim()
        android.util.Log.d("GeminiOCR", "extractTextFromImage 결과 길이=${out.length}")
        out
    }

    /**
     * 지문 내용 이해도를 확인하는 4지선다 객관식 문제 JSON 배열을 생성합니다.
     * 반환 예: [{"question":"...","options":["a","b","c","d"],"answerIndex":0}, ...]
     */
    suspend fun generateComprehensionQuiz(passage: String, count: Int = 3): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) return@withContext ""
        val prompt = content {
            text(
                "다음 지문을 읽고 '내용 이해도'를 확인하는 4지선다 객관식 문제 ${count}개를 만들어 주세요. " +
                "지문에 실제로 나온 내용만으로 출제하고, 각 문제는 보기 4개와 정답 1개(answerIndex는 0부터 시작)를 가집니다. " +
                "반드시 아래 JSON 배열 형식으로만 응답하세요(설명·코드블록·마크다운 금지):\n" +
                "[{\"question\":\"질문\",\"options\":[\"보기1\",\"보기2\",\"보기3\",\"보기4\"],\"answerIndex\":0}]\n\n" +
                "지문:\n$passage"
            )
        }
        val response = generativeModel?.generateContent(prompt)
        (response?.text ?: "").trim()
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
            text("당신은 전문 교육용 퀴즈 출제 위원입니다. 다음 주제나 텍스트를 바탕으로 객관식 및 주관식 퀴즈를 ${count}개 생성해 주세요. 만약 입력된 텍스트가 단어 목록(단어장) 형태라면, 리스트에 등장하는 단어들을 최대한 골고루 정답과 문제로 활용해 퀴즈를 구성해 주세요.\n\n" +
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

        return@withContext try
        {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text
            if (result.isNullOrBlank())
            {
                throw Exception("AI 응답이 비어 있습니다. (텍스트가 너무 짧거나 안전 정책에 의해 차단되었을 수 있습니다.)")
            }
            cleanJsonString(result)
        }
        catch (e: Exception)
        {
            android.util.Log.e("GeminiManager", "❌ generateQuizFromText error: ${e.message}", e)
            throw e
        }
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
                "당신은 전문 교육용 퀴즈 출제 위원입니다. 제공된 이미지(교과서, 문제집, 단어장 등)들을 정밀 스캔하여 고도의 완성도를 지닌 퀴즈를 ${count}개 생성해 주세요. 만약 이미지에 단어장(단어 목록)이나 단어 정리표가 포함되어 있다면, 목록에 등장하는 단어들과 그 뜻을 적극적으로 추출하여 중복 없이 골고루 문제와 정답으로 출제해 주세요.\n\n" +
                "이전 출제되었던 문제 목록(중복 방지용):\n$previousQuizzesJson\n\n" +
                "사용자의 학습 분석 오답 기록(취약 영역 저격용):\n$errorStatsJson\n\n" +
                "제약 사항 및 요구 조건:\n" +
                "1. **저작권 보호 및 재창작(Paraphrasing) 의무**:\n" +
                "   - 이미지 안의 본문 텍스트를 절대로 그대로 타이핑하여 복제(Verbatim Copy)하지 마세요.\n" +
                "   - 반드시 이미지 안의 지식과 개념을 파악하여 질문과 보기, 해설을 **독창적으로 요약 및 변형 가공**하여 새로운 문제로 재창작해 주세요.\n" +
                "2. **맥락 기반 중복 방지**:\n" +
                "   - 제공된 '이전 출제되었던 문제 목록'에 있는 질문들과 유사하거나 중복되는 문제가 출제되지 않도록 완전히 새로운 문제를 설계하세요.\n" +
                "   - 특히 질문을 다르게 바꾸더라도 '정답(answer)'이 목록에 이미 있는 것과 같으면 절대 출제하지 마세요. 반드시 목록에 없는 새로운 정답이 나오는 문제만 만드세요.\n" +
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
            val result = response?.text
            if (result.isNullOrBlank())
            {
                throw Exception("AI 응답이 비어 있습니다. (이미지에서 텍스트를 인식하지 못했거나 안전 정책에 의해 차단되었을 수 있습니다.)")
            }
            cleanJsonString(result)
        }
        catch (e: Exception)
        {
            android.util.Log.e("GeminiManager", "❌ generateQuizzesFromImages error: ${e.message}", e)
            throw e
        }
    }

    /**
     * 다중 이미지 스캔을 통해 그림들의 위치(Bounding Box)를 추출하고 이미지 크롭용 퀴즈 데이터셋을 생성합니다.
     */
    suspend fun generateVisualQuizzesFromImages(
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
                "당신은 이미지 분석 및 시각 교육용 퀴즈 출제 위원입니다. 제공된 이미지(교과서, 문제집, 단어장 등) 속에서 개별 사물, 그림, 도형, 캐릭터 등 퀴즈 문제로 낼 수 있는 그림 요소들을 정밀 탐지하여 퀴즈를 ${count}개 생성해 주세요.\n\n" +
                "이전 출제되었던 문제 목록(중복 방지용):\n$previousQuizzesJson\n\n" +
                "사용자의 학습 분석 오답 기록(취약 영역 저격용):\n$errorStatsJson\n\n" +
                "핵심 요구 조건:\n" +
                "0. **중복 방지**: '이전 출제되었던 문제 목록'에 있는 질문·정답과 겹치지 않는, 새로운 그림 대상만 출제하세요.\n" +
                "1. **그림 영역 탐지 (Bounding Box)**:\n" +
                "   - 퀴즈 질문의 대상이 되는 개별 그림/일러스트(글자 텍스트 영역 제외, 순수 그림 영역)의 2D 바운딩 박스 좌표를 정확히 감지해 주세요.\n" +
                "   - 좌표계 형식: [ymin, xmin, ymax, xmax] (0부터 1000 사이의 정수 비율로 표현합니다. 예: 이미지 맨 위가 0, 맨 아래가 1000 / 맨 왼쪽이 0, 맨 오른쪽이 1000)\n" +
                "   - **주의**: 감지할 이미지 인덱스는 순서대로 첫 번째 이미지 기준입니다. 만약 이미지가 여러 장이면 해당하는 그림이 위치한 이미지의 경계를 기준으로 하세요.\n" +
                "2. **퀴즈 콘텐츠 재창작**:\n" +
                "   - 질문 내용은 그 그림을 보고 유추할 수 있는 문제여야 합니다. (예: '이 그림이 나타내는 과일의 영어 이름은?', '다음 일러스트의 명칭은 무엇인가요?')\n" +
                "   - 정답은 그림의 실제 대상이 되는 단어나 숫자, 설명이어야 합니다. (예: 'apricot', '사과' 등)\n" +
                "3. **완벽한 JSON 배열 형식 응답**:\n" +
                "   - 반드시 다음 JSON 형식의 배열로만 응답하세요. 다른 설명이나 텍스트는 제외하세요.\n" +
                "   - JSON 구조:\n" +
                "     [\n" +
                "       {\n" +
                "         \"id\": 숫자,\n" +
                "         \"type\": \"MULTIPLE_CHOICE\" 또는 \"SUBJECTIVE\",\n" +
                "         \"category\": \"AI 이미지 퀴즈\",\n" +
                "         \"subCategory\": \"그림 매칭\",\n" +
                "         \"question\": \"질문 내용 (예: 이 그림이 나타내는 과일은 무엇일까요?)\",\n" +
                "         \"options\": [\"보기1\", \"보기2\", \"보기3\", \"보기4\"] (주관식인 경우 null, 반드시 보기 중에 정답이 포함되어 있어야 함),\n" +
                "         \"answer\": \"정답\",\n" +
                "         \"explanation\": \"상세 해설\",\n" +
                "         \"semanticHint\": \"힌트 (예: a로 시작하는 단어)\",\n" +
                "         \"boundingBox\": [ymin, xmin, ymax, xmax] (예: [100, 700, 180, 760] 과 같이 4개의 정수로 구성된 배열)\n" +
                "       }\n" +
                "     ]\n" +
                "4. 주관식의 경우 한두 단어의 매우 간단한 명칭이 정답이 되도록 설계하세요."
            )
        }

        return@withContext try
        {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text
            if (result.isNullOrBlank())
            {
                throw Exception("AI 응답이 비어 있습니다. (이미지에서 그림 좌표를 인식하지 못했거나 안전 정책에 의해 차단되었을 수 있습니다.)")
            }
            cleanJsonString(result)
        }
        catch (e: Exception)
        {
            android.util.Log.e("GeminiManager", "❌ generateVisualQuizzesFromImages error: ${e.message}", e)
            throw e
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

    /**
     * DART 정형 재무 JSON을 기반으로 어닝 서프라이즈 여부 판정 및 3줄 요약 Markdown을 생성합니다.
     */
    suspend fun verifyEarningsDisclosure(rawFinancialJson: String): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext ""
        }

        val prompt = content {
            text(
                "당신은 대한민국 코스닥/코스피 시장 전문 금융 분석가이자 퀀트(Quant)입니다. 아래 제공된 JSON 형태의 기업 실적 데이터를 분석하여 정돈된 요약 보고서를 작성하고, 실적 등급을 판정해 주세요.\n\n" +
                "실적 데이터:\n$rawFinancialJson\n\n" +
                "중요 지침:\n" +
                "- 만약 JSON에 'error' 키가 있고 재무 수치(revenue, operating_profit 등)가 없다면, 이는 잠정실적 발표 또는 비정형 공시로 DART 재무 API에 세부 수치가 아직 없는 경우입니다.\n" +
                "- 이 경우에도 반드시 JSON 형식으로 응답하되, summary에 '재무 세부 수치를 확인할 수 없는 공시 유형(잠정실적 또는 사전 공시)입니다. DART 원문을 직접 확인하시기 바랍니다.'와 함께 해당 공시 유형의 일반적 투자 시사점을 작성하세요.\n" +
                "- 재무 데이터가 있는 경우 전년 대비 변동률과 서프라이즈 여부를 정확히 분석하세요.\n\n" +
                "요구 조건:\n" +
                "1. 반드시 다음 JSON 객체 형식으로만 응답하세요. 다른 부가적인 텍스트나 코드 펜스는 제외하세요.\n" +
                "   JSON 구조:\n" +
                "   {\n" +
                "     \"isSurprise\": true 또는 false 또는 null (매출액 및 영업이익이 전년 대비 대폭 성장했거나 시장 예상치를 상회하는 경우 true, 전년 대비 대폭 감소했거나 어닝 쇼크인 경우 false, 예상 수준에 부합하거나 미미한 변동인 경우 null),\n" +
                "     \"isTurnaround\": true 또는 false (영업이익이나 순이익이 이전 기간 적자에서 이번 기간에 흑자로 전환된 경우 true, 그 외에는 false),\n" +
                "     \"summary\": \"### 📊 실적 요약\\n* **매출액 변동**: 전년비 변동률 및 금액 기재\\n* **영업이익 변동**: 전년비 변동률 및 금액, 흑자전환 여부 필수 기재\\n\\n### 💡 3줄 투자 관점\\n1. 이번 실적의 가장 긍정적인 요인\\n2. 주의 깊게 봐야 할 리스크 또는 비용 요인\\n3. 종합 평가: 어닝 서프라이즈 / 인라인 / 어닝 쇼크 중 하나를 선택하고 그 이유를 설명\"\n" +
                "   }\n" +
                "2. 반드시 유효한 JSON 문자열만 반환하세요."
            )
        }

        return@withContext try {
            val response = generativeModel?.generateContent(prompt)
            val result = response?.text ?: ""
            cleanJsonString(result)
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "❌ verifyEarningsDisclosure error: ${e.message}", e)
            ""
        }
    }

    /**
     * 실적 공시 발표 예정 종목에 대한 AI 사전 전망 리포트를 생성합니다.
     */
    suspend fun generateExpectedEarningsReport(
        companyName: String,
        consensusRevenue: String,
        consensusProfit: String
    ): String = withContext(Dispatchers.IO) {
        if (generativeModel == null || apiKey.isNullOrBlank()) {
            return@withContext "API 키를 설정하면 AI 사전 전망 리포트를 생성할 수 있습니다."
        }

        val prompt = content {
            text(
                "당신은 대한민국 주식시장 전문 금융 애널리스트입니다. 이번 주 실적 발표가 예정된 '$companyName' 기업의 사전 전망 리포트를 작성해 주세요.\n\n" +
                "참고 데이터:\n" +
                "- 기업명: $companyName\n" +
                "- 시장 예상 매출액(컨센서스): $consensusRevenue\n" +
                "- 시장 예상 영업이익(컨센서스): $consensusProfit\n\n" +
                "요구 조건:\n" +
                "1. 친절하고 전문적인 말투의 한국어로 작성하세요.\n" +
                "2. 아래 형식을 지켜 마크다운(Markdown) 포맷으로 답변하세요:\n\n" +
                "### 🗓️ $companyName 실적 발표 사전 관전 포인트\n" +
                "* **시장 예상치(컨센서스)**: 매출액 및 영업이익 전망 요약\n" +
                "* **최근 주요 이슈 및 업황**: 최근 업계 트렌드 및 기업 동향 분석\n" +
                "* **발표 시 주목해야 할 핵심 지표**:\n" +
                "  1. [핵심 포인트 1]\n" +
                "  2. [핵심 포인트 2]"
            )
        }

        return@withContext try {
            val response = generativeModel?.generateContent(prompt)
            response?.text ?: "리포트를 생성할 수 없습니다."
        } catch (e: Exception) {
            android.util.Log.e("GeminiManager", "❌ generateExpectedEarningsReport error: ${e.message}", e)
            "사전 리포트 생성 중 오류가 발생했습니다: ${e.message}"
        }
    }
}
