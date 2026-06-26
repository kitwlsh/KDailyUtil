package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class QuizRepository {

    private val REMOTE_BASE_URL = "https://raw.githubusercontent.com/kitwlsh/korean_quiz_data/refs/heads/main/"
    private val QUIZ_FILES = listOf("korean.json", "trend.json", "knowledge.json", "travel.json", "quiz_updates.json")
    private val QUIZ_CACHE_FILE = "quizzes_v2.json"
    private val CUSTOM_QUIZ_FILE = "custom_quizzes.json"
    private val TAG = "QuizRepository"

    // 중복 판정용 정규화: 공백 제거 + 소문자
    private fun norm(s: String?): String = (s ?: "").replace(Regex("\\s+"), "").lowercase()

    // 같은 카테고리 안에서 정답 또는 질문이 이미 본 것과 같으면 중복으로 본다.
    private fun answerKey(q: QuizQuestion) = q.category to norm(q.answer)
    private fun questionKey(q: QuizQuestion) = q.category to norm(q.question)

    /**
     * (카테고리, 정답)·(카테고리, 질문) 기준으로 중복을 제거한다. 첫 등장만 유지.
     * 이미지 퀴즈처럼 정답 텍스트가 비어 있을 수 있는 경우(imageUrl 존재)는 질문 기준만 적용.
     */
    private fun dedupeQuizzes(quizzes: List<QuizQuestion>): List<QuizQuestion> {
        val seenAns = HashSet<Pair<String, String>>()
        val seenQ = HashSet<Pair<String, String>>()
        val result = ArrayList<QuizQuestion>(quizzes.size)
        for (q in quizzes) {
            val qk = questionKey(q)
            if (qk in seenQ) continue
            // 시각(이미지) 퀴즈는 정답이 같아도 이미지가 다를 수 있으므로 질문 기준만 적용
            val useAnswer = norm(q.answer).isNotBlank() && q.imageUrl.isNullOrBlank()
            val ak = answerKey(q)
            if (useAnswer && ak in seenAns) continue
            seenQ.add(qk)
            if (useAnswer) seenAns.add(ak)
            result.add(q)
        }
        return result
    }

    // 기존의 하드코딩된 기본 문제들 (인터넷 안 될 때를 대비한 뼈대)
    private fun getStaticQuizzes(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(1, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "다음 중 맞춤법이 올바른 것은?", listOf("어의가 없다", "어이가 없다", "어의가 읍다", "어이가 읍다"), "어이가 없다", "'어이'가 바른 표기입니다. '어의'는 조선시대 임금의 병을 치료하던 의원을 뜻합니다.", "'어처구니'와 같은 말로, 뜻밖의 일이 일어났을 때 씁니다."),
            QuizQuestion(2, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "다음 중 올바른 표기는?", listOf("며칠", "몇 일", "몇일", "며 일"), "며칠", "'몇 일'은 쓰이지 않으며 항상 '며칠'로 적는 것이 맞춤법 규정입니다.", "그달의 몇째 되는 날을 뜻합니다."),
            QuizQuestion(3, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "어휘", "찌개가 보글보글 ( ).", listOf("졸이다", "졸히다", "조리다", "끓이다"), "끓이다", "찌개는 '끓이다'가 자연스러우며, 국물이 줄어들게 하는 것은 '졸이다'입니다.", "액체가 뜨거워져 거품이 솟아오르게 한다는 뜻입니다."),
            QuizQuestion(4, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "어휘", "김치찌개를 ( ).", listOf("졸이다", "조리다", "졸히다", "절이다"), "조리다", "고기나 생선 등을 양념하여 국물에 바짝 끓이는 것은 '조리다'입니다.", "양념이 배어들게 하는 조리 방식입니다."),
            QuizQuestion(5, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "다음 중 올바른 표기는?", listOf("설레임", "설렘", "설램", "설래임"), "설렘", "기본형 '설레다'의 명사형은 '설렘'입니다.", "마음이 들떠서 두근거린다는 뜻의 명사형입니다."),
            QuizQuestion(6, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "순우리말", "어제 친구와 ( ) 다퉜다.", listOf("티격태격", "티격대격", "티격테격", "태격태격"), "티격태격", "뜻이 맞지 않아 가벼운 말다툼을 하는 모양은 '티격태격'입니다.", "가볍게 다투는 모양을 나타내는 부사입니다."),
            QuizQuestion(7, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "다음 중 올바른 표기는?", listOf("금새", "금세", "금시", "금새에"), "금세", "'금세'는 '금시에'가 줄어든 말입니다.", "'지금 바로'라는 뜻으로 쓰입니다."),
            QuizQuestion(8, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "그는 책임을 ( ) 도망쳤다.", listOf("안고", "앉고", "않고", "안코"), "안고", "'책임을 지다'의 뜻을 가진 '안다'의 활용형입니다.", "어떤 일이나 책임을 맡는다는 뜻입니다."),
            QuizQuestion(9, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "다음 중 올바른 표기는?", listOf("오랫만에", "오랜만에", "오랫동안에", "오랜동안에"), "오랜만에", "'오래간만'의 준말이므로 '오랜만'이 맞습니다.", "오래간만에 라는 뜻입니다."),
            QuizQuestion(10, QuizType.MULTIPLE_CHOICE, "우리말 겨루기", "맞춤법", "문을 꽉 ( ).", listOf("잠궈라", "잠가라", "잠구어라", "잠거라"), "잠가라", "기본형 '잠그다'의 어간에 '아/어'가 붙을 때 '으'가 탈락합니다.", "열리지 않도록 자물쇠를 채우다라는 뜻입니다."),
            QuizQuestion(11, QuizType.SUBJECTIVE, "우리말 겨루기", "고유어", "[주관식] 하루, 이틀, (  ), 나흘. 빈칸에 들어갈 3일을 뜻하는 순우리말은?", null, "사흘", "3일은 '사흘'입니다.", "달의 셋째 되는 날을 뜻하기도 합니다."),
            QuizQuestion(12, QuizType.SUBJECTIVE, "우리말 겨루기", "사자성어", "[주관식] 마음이 초조하고 불안하여 어찌할 바를 모르는 모양을 뜻하는 네 글자는?", null, "안절부절", "초조하고 불안한 모양은 '안절부절'입니다.", "이 말 뒤에는 주로 '못하다'가 붙습니다."),
            QuizQuestion(13, QuizType.SUBJECTIVE, "우리말 겨루기", "사자성어", "[주관식] 앞에서는 복종하는 체하면서 뒤에서는 배반함을 이르는 사자성어는?", null, "면종복배", "앞에서는 복종하는 체하면서 뒤에서는 배반함을 이릅니다.", "얼굴 면, 따를 종, 배 복, 등 배 한자를 씁니다."),
            QuizQuestion(14, QuizType.SUBJECTIVE, "우리말 겨루기", "고유어", "[주관식] '4일'을 의미하는 순우리말은?", null, "나흘", "4일은 나흘입니다.", "달의 넷째 되는 날을 뜻하기도 합니다."),
            QuizQuestion(15, QuizType.SUBJECTIVE, "우리말 겨루기", "사자성어", "[주관식] 바람 앞의 등불이라는 뜻으로, 매우 위태로운 처지를 비유하는 사자성어는?", null, "풍전등화", "바람 앞의 등불이라는 뜻입니다.", "바람 풍, 앞 전, 등불 등, 불 화 한자를 씁니다.")
        )
    }

    /**
     * 앱 시작 시 또는 특정 화면 진입 시 호출하여 인터넷에서 최신 JSON을 다운로드해 로컬에 캐시합니다.
     */
    suspend fun syncRemoteQuizzes(context: Context) = withContext(Dispatchers.IO) {
        val allRemoteQuizzes = mutableListOf<QuizQuestion>()
        
        for (fileName in QUIZ_FILES) {
            try {
                val url = URL(REMOTE_BASE_URL + fileName)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    val quizzes = parseQuizzes(jsonString)
                    allRemoteQuizzes.addAll(quizzes)
                    Log.d(TAG, "✅ Synced $fileName: ${quizzes.size} questions")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to sync $fileName: ${e.message}")
            }
        }

        if (allRemoteQuizzes.isNotEmpty()) {
            val file = File(context.filesDir, QUIZ_CACHE_FILE)
            val jsonArray = JSONArray()
            allRemoteQuizzes.forEach { q ->
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("type", q.type.name)
                    put("category", q.category)
                    put("subCategory", q.subCategory)
                    put("question", q.question)
                    put("answer", q.answer)
                    put("explanation", q.explanation)
                    put("semanticHint", q.semanticHint)
                    put("imageUrl", q.imageUrl)
                    q.options?.let { put("options", JSONArray(it)) }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
            Log.d(TAG, "🚀 Total ${allRemoteQuizzes.size} quizzes saved to cache.")
        }
    }

    private fun parseQuizzes(jsonText: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        val jsonArray = JSONArray(jsonText)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val optionsArray = obj.optJSONArray("options")
            val optionsList = if (optionsArray != null) {
                List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
            } else null
            list.add(
                QuizQuestion(
                    id = obj.getInt("id"),
                    type = QuizType.valueOf(obj.getString("type")),
                    category = obj.getString("category"),
                    subCategory = obj.optString("subCategory", ""),
                    question = obj.getString("question"),
                    options = optionsList,
                    answer = obj.getString("answer"),
                    explanation = obj.getString("explanation"),
                    semanticHint = obj.optString("semanticHint", ""),
                    imageUrl = obj.optString("imageUrl", "")
                )
            )
        }
        return list
    }

    /**
     * 게임을 시작할 때 로컬에 저장된 퀴즈 리스트를 가져옵니다.
     * 캐시 파일이 없으면 하드코딩된 정적 퀴즈를 반환합니다.
     */
    suspend fun getQuizzes(context: Context, category: String? = null): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val staticQuizzes = getStaticQuizzes()
        val file = File(context.filesDir, QUIZ_CACHE_FILE)
        
        val allQuizzes = mutableListOf<QuizQuestion>()
        allQuizzes.addAll(staticQuizzes)

        if (file.exists()) {
            try {
                val jsonText = file.readText()
                val dynamicQuizzes = parseQuizzes(jsonText)
                
                // 기존 문제와 원격 문제를 ID 기준으로 합침 (원격 우선)
                val finalMap = staticQuizzes.associateBy { it.id }.toMutableMap()
                dynamicQuizzes.forEach { finalMap[it.id] = it }
                allQuizzes.clear()
                allQuizzes.addAll(finalMap.values)

            } catch (e: Exception) {
                Log.e(TAG, "로컬 캐시 퀴즈 파싱 오류: ${e.message}")
            }
        }

        // 커스텀 퀴즈 합산
        val customFile = File(context.filesDir, CUSTOM_QUIZ_FILE)
        if (customFile.exists())
        {
            try
            {
                val customQuizzes = parseQuizzes(customFile.readText())
                allQuizzes.addAll(customQuizzes)
            }
            catch (e: Exception)
            {
                Log.e(TAG, "로컬 커스텀 퀴즈 파싱 오류: ${e.message}")
            }
        }
        
        // 카테고리 필터링 적용
        val filtered = if (category != null)
        {
            allQuizzes.filter { it.category == category }
        }
        else
        {
            allQuizzes
        }

        // 표시 단계 안전망: static/remote/custom 어디서 왔든 같은 카테고리의 정답·질문 중복은 한 번만 노출
        return@withContext dedupeQuizzes(filtered).shuffled()
    }

    /**
     * 커스텀 퀴즈를 로컬 custom_quizzes.json 파일에 저장합니다.
     */
    suspend fun saveCustomQuizzes(context: Context, quizzes: List<QuizQuestion>) = withContext(Dispatchers.IO)
    {
        val file = File(context.filesDir, CUSTOM_QUIZ_FILE)
        try
        {
            val existing = if (file.exists())
            {
                parseQuizzes(file.readText()).toMutableList()
            }
            else
            {
                mutableListOf()
            }
            
            // ID 기준 병합 + 제출 단계 중복 방지:
            // 같은 카테고리에 이미 있는(다른 ID) 퀴즈와 정답/질문이 겹치면 저장하지 않는다.
            // (같은 ID는 기존 항목 '갱신'으로 보고 허용)
            val mergedMap = existing.associateBy { it.id }.toMutableMap()
            val ansOwner = HashMap<Pair<String, String>, Int>()
            val qOwner = HashMap<Pair<String, String>, Int>()
            existing.forEach { e ->
                qOwner[questionKey(e)] = e.id
                if (norm(e.answer).isNotBlank() && e.imageUrl.isNullOrBlank()) ansOwner[answerKey(e)] = e.id
            }
            var skipped = 0
            quizzes.forEach { q ->
                val qk = questionKey(q)
                val useAnswer = norm(q.answer).isNotBlank() && q.imageUrl.isNullOrBlank()
                val ak = answerKey(q)
                val dupQ = qOwner[qk]?.let { it != q.id } ?: false
                val dupA = if (useAnswer) (ansOwner[ak]?.let { it != q.id } ?: false) else false
                if (dupQ || dupA) {
                    skipped++
                    Log.d(TAG, "↪ 중복 퀴즈 저장 건너뜀: '${q.answer}' (${q.question.take(24)}...)")
                    return@forEach
                }
                mergedMap[q.id] = q
                qOwner[qk] = q.id
                if (useAnswer) ansOwner[ak] = q.id
            }

            val jsonArray = JSONArray()
            mergedMap.values.forEach { q ->
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("type", q.type.name)
                    put("category", q.category)
                    put("subCategory", q.subCategory)
                    put("question", q.question)
                    put("answer", q.answer)
                    put("explanation", q.explanation)
                    put("semanticHint", q.semanticHint ?: "")
                    put("imageUrl", q.imageUrl ?: "")
                    q.options?.let { opt ->
                        put("options", JSONArray(opt))
                    }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
            Log.d(TAG, "💾 Saved ${mergedMap.size} custom quizzes to local store. (중복 ${skipped}개 건너뜀)")
        }
        catch (e: Exception)
        {
            Log.e(TAG, "❌ Failed to save custom quizzes: ${e.message}")
        }
    }

    /**
     * 특정 커스텀 카테고리를 완전히 삭제합니다.
     */
    suspend fun deleteCustomCategory(context: Context, category: String) = withContext(Dispatchers.IO)
    {
        val file = File(context.filesDir, CUSTOM_QUIZ_FILE)
        if (!file.exists())
        {
            return@withContext
        }
        try
        {
            val existing = parseQuizzes(file.readText())
            val toDelete = existing.filter { it.category == category }
            val filtered = existing.filter { it.category != category }
            
            // Delete associated cropped image files
            toDelete.forEach { q ->
                if (!q.imageUrl.isNullOrBlank()) {
                    val imgFile = File(q.imageUrl)
                    if (imgFile.exists() && q.imageUrl.contains("cropped_quizzes")) {
                        try {
                            val deleted = imgFile.delete()
                            Log.d(TAG, "🗑 Cleaned up cropped image: ${q.imageUrl}, result: $deleted")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to delete crop image file: ${q.imageUrl}, error: ${e.message}")
                        }
                    }
                }
            }
            
            val jsonArray = JSONArray()
            filtered.forEach { q ->
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("type", q.type.name)
                    put("category", q.category)
                    put("subCategory", q.subCategory)
                    put("question", q.question)
                    put("answer", q.answer)
                    put("explanation", q.explanation)
                    put("semanticHint", q.semanticHint ?: "")
                    put("imageUrl", q.imageUrl ?: "")
                    q.options?.let { opt ->
                        put("options", JSONArray(opt))
                    }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
            Log.d(TAG, "🗑 Deleted custom category: $category. Remaining: ${filtered.size}")
        }
        catch (e: Exception)
        {
            Log.e(TAG, "❌ Failed to delete custom category: ${e.message}")
        }
    }

    /**
     * 로컬에 등록된 커스텀 퀴즈들의 유니크한 카테고리 목록을 반환합니다.
     */
    suspend fun getCustomCategories(context: Context): List<String> = withContext(Dispatchers.IO)
    {
        val file = File(context.filesDir, CUSTOM_QUIZ_FILE)
        if (!file.exists())
        {
            return@withContext emptyList()
        }
        try
        {
            val quizzes = parseQuizzes(file.readText())
            return@withContext quizzes.map { it.category }.distinct()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "❌ Failed to parse custom categories: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * 원격에서 동기화되어 캐시된 퀴즈들의 유니크한 카테고리 목록을 반환합니다.
     */
    suspend fun getRemoteCategories(context: Context): List<String> = withContext(Dispatchers.IO)
    {
        val file = File(context.filesDir, QUIZ_CACHE_FILE)
        if (!file.exists())
        {
            return@withContext emptyList()
        }
        try
        {
            val quizzes = parseQuizzes(file.readText())
            return@withContext quizzes.map { it.category }.distinct()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "❌ Failed to parse remote categories: ${e.message}")
            return@withContext emptyList()
        }
    }
}
