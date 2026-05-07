package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

class QuizRepository {

    // 향후 개발자님이 GitHub 등에 올린 원격 JSON 파일 주소를 여기에 적습니다.
    private val REMOTE_JSON_URL = "https://raw.githubusercontent.com/kitwlsh/korean_quiz_data/refs/heads/main/quiz_updates.json"
    private val LOCAL_CACHE_FILE = "quiz_cache.json"
    private val TAG = "QuizRepository"

    // 기존의 하드코딩된 기본 문제들 (인터넷 안 될 때를 대비한 뼈대)
    private fun getStaticQuizzes(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(1, QuizType.MULTIPLE_CHOICE, "맞춤법", "다음 중 맞춤법이 올바른 것은?", listOf("어의가 없다", "어이가 없다", "어의가 읍다", "어이가 읍다"), "어이가 없다", "'어이'가 바른 표기입니다. '어의'는 조선시대 임금의 병을 치료하던 의원을 뜻합니다.", "'어처구니'와 같은 말로, 뜻밖의 일이 일어났을 때 씁니다."),
            QuizQuestion(2, QuizType.MULTIPLE_CHOICE, "맞춤법", "다음 중 올바른 표기는?", listOf("며칠", "몇 일", "몇일", "며 일"), "며칠", "'몇 일'은 쓰이지 않으며 항상 '며칠'로 적는 것이 맞춤법 규정입니다.", "그달의 몇째 되는 날을 뜻합니다."),
            QuizQuestion(3, QuizType.MULTIPLE_CHOICE, "어휘", "찌개가 보글보글 ( ).", listOf("졸이다", "졸히다", "조리다", "끓이다"), "끓이다", "찌개는 '끓이다'가 자연스러우며, 국물이 줄어들게 하는 것은 '졸이다'입니다.", "액체가 뜨거워져 거품이 솟아오르게 한다는 뜻입니다."),
            QuizQuestion(4, QuizType.MULTIPLE_CHOICE, "어휘", "김치찌개를 ( ).", listOf("졸이다", "조리다", "졸히다", "절이다"), "조리다", "고기나 생선 등을 양념하여 국물에 바짝 끓이는 것은 '조리다'입니다.", "양념이 배어들게 하는 조리 방식입니다."),
            QuizQuestion(5, QuizType.MULTIPLE_CHOICE, "맞춤법", "다음 중 올바른 표기는?", listOf("설레임", "설렘", "설램", "설래임"), "설렘", "기본형 '설레다'의 명사형은 '설렘'입니다.", "마음이 들떠서 두근거린다는 뜻의 명사형입니다."),
            QuizQuestion(6, QuizType.MULTIPLE_CHOICE, "순우리말", "어제 친구와 ( ) 다퉜다.", listOf("티격태격", "티격대격", "티격테격", "태격태격"), "티격태격", "뜻이 맞지 않아 가벼운 말다툼을 하는 모양은 '티격태격'입니다.", "가볍게 다투는 모양을 나타내는 부사입니다."),
            QuizQuestion(7, QuizType.MULTIPLE_CHOICE, "맞춤법", "다음 중 올바른 표기는?", listOf("금새", "금세", "금시", "금새에"), "금세", "'금세'는 '금시에'가 줄어든 말입니다.", "'지금 바로'라는 뜻으로 쓰입니다."),
            QuizQuestion(8, QuizType.MULTIPLE_CHOICE, "맞춤법", "그는 책임을 ( ) 도망쳤다.", listOf("안고", "앉고", "않고", "안코"), "안고", "'책임을 지다'의 뜻을 가진 '안다'의 활용형입니다.", "어떤 일이나 책임을 맡는다는 뜻입니다."),
            QuizQuestion(9, QuizType.MULTIPLE_CHOICE, "맞춤법", "다음 중 올바른 표기는?", listOf("오랫만에", "오랜만에", "오랫동안에", "오랜동안에"), "오랜만에", "'오래간만'의 준말이므로 '오랜만'이 맞습니다.", "오래간만에 라는 뜻입니다."),
            QuizQuestion(10, QuizType.MULTIPLE_CHOICE, "맞춤법", "문을 꽉 ( ).", listOf("잠궈라", "잠가라", "잠구어라", "잠거라"), "잠가라", "기본형 '잠그다'의 어간에 '아/어'가 붙을 때 '으'가 탈락합니다.", "열리지 않도록 자물쇠를 채우다라는 뜻입니다."),
            QuizQuestion(11, QuizType.SUBJECTIVE, "고유어", "[주관식] 하루, 이틀, (  ), 나흘. 빈칸에 들어갈 3일을 뜻하는 순우리말은?", null, "사흘", "3일은 '사흘'입니다.", "달의 셋째 되는 날을 뜻하기도 합니다."),
            QuizQuestion(12, QuizType.SUBJECTIVE, "사자성어", "[주관식] 마음이 초조하고 불안하여 어찌할 바를 모르는 모양을 뜻하는 네 글자는?", null, "안절부절", "초조하고 불안한 모양은 '안절부절'입니다.", "이 말 뒤에는 주로 '못하다'가 붙습니다."),
            QuizQuestion(13, QuizType.SUBJECTIVE, "사자성어", "[주관식] 앞에서는 복종하는 체하면서 뒤에서는 배반함을 이르는 사자성어는?", null, "면종복배", "앞에서는 복종하는 체하면서 뒤에서는 배반함을 이릅니다.", "얼굴 면, 따를 종, 배 복, 등 배 한자를 씁니다."),
            QuizQuestion(14, QuizType.SUBJECTIVE, "고유어", "[주관식] '4일'을 의미하는 순우리말은?", null, "나흘", "4일은 나흘입니다.", "달의 넷째 되는 날을 뜻하기도 합니다."),
            QuizQuestion(15, QuizType.SUBJECTIVE, "사자성어", "[주관식] 바람 앞의 등불이라는 뜻으로, 매우 위태로운 처지를 비유하는 사자성어는?", null, "풍전등화", "바람 앞의 등불이라는 뜻입니다.", "바람 풍, 앞 전, 등불 등, 불 화 한자를 씁니다.")
        )
    }

    /**
     * 앱 시작 시 또는 특정 화면 진입 시 호출하여 인터넷에서 최신 JSON을 다운로드해 로컬에 캐시합니다.
     */
    suspend fun syncRemoteQuizzes(context: Context) = withContext(Dispatchers.IO) {
        try {
            // 인터넷 연결을 통해 JSON 텍스트 가져오기
            val jsonText = URL(REMOTE_JSON_URL).readText()
            
            // 로컬 파일로 덮어쓰기 저장 (캐싱)
            val file = File(context.filesDir, LOCAL_CACHE_FILE)
            file.writeText(jsonText)
            Log.d(TAG, "원격 퀴즈 데이터 동기화 성공!")
        } catch (e: Exception) {
            Log.e(TAG, "원격 퀴즈 데이터 동기화 실패 (오프라인이거나 URL 오류): ${e.message}")
        }
    }

    /**
     * 게임을 시작할 때 로컬에 저장된 퀴즈 리스트를 가져옵니다.
     * 캐시 파일이 없으면 하드코딩된 정적 퀴즈를 반환합니다.
     */
    suspend fun getQuizzes(context: Context): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val staticQuizzes = getStaticQuizzes()
        val file = File(context.filesDir, LOCAL_CACHE_FILE)

        if (file.exists()) {
            try {
                val jsonText = file.readText()
                val jsonArray = JSONArray(jsonText)
                val dynamicQuizzes = mutableListOf<QuizQuestion>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val optionsArray = obj.optJSONArray("options")
                    val optionsList = if (optionsArray != null) {
                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                    } else null

                    dynamicQuizzes.add(
                        QuizQuestion(
                            id = obj.getInt("id"),
                            type = QuizType.valueOf(obj.getString("type")),
                            category = obj.getString("category"),
                            question = obj.getString("question"),
                            options = optionsList,
                            answer = obj.getString("answer"),
                            explanation = obj.getString("explanation"),
                            semanticHint = obj.optString("semanticHint", null)
                        )
                    )
                }
                
                // 기존 문제와 원격 문제를 ID 기준으로 합침 (원격 우선)
                val finalMap = staticQuizzes.associateBy { it.id }.toMutableMap()
                dynamicQuizzes.forEach { finalMap[it.id] = it }
                
                return@withContext finalMap.values.toList().shuffled()

            } catch (e: Exception) {
                Log.e(TAG, "로컬 캐시 퀴즈 파싱 오류: ${e.message}")
                return@withContext staticQuizzes.shuffled()
            }
        } else {
            return@withContext staticQuizzes.shuffled()
        }
    }
}
