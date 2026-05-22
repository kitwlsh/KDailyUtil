package com.kitwlshcom.kdailyutil.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class QuestionStats(
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
)

class QuizStatsManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val statsFile = File(appContext.filesDir, STATS_FILE_NAME)
    private val statsMap = mutableMapOf<String, QuestionStats>()
    private val TAG = "QuizStatsManager"

    init {
        loadStats()
    }

    companion object {
        private const val STATS_FILE_NAME = "quiz_stats.json"
        
        @Volatile
        private var INSTANCE: QuizStatsManager? = null

        fun getInstance(context: Context): QuizStatsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = QuizStatsManager(context)
                INSTANCE = instance
                instance
            }
        }

        /**
         * 카테고리와 질문 본문을 활용하여 충돌이 발생하지 않는 고유 문자열 해시 키를 빌드합니다.
         */
        fun getUniqueKey(category: String, question: String): String {
            val cleanCategory = category.trim().replace("\\s+".toRegex(), "_")
            val questionHash = question.trim().hashCode()
            return "${cleanCategory}_$questionHash"
        }
    }

    /**
     * 로컬 quiz_stats.json 파일로부터 통계 데이터를 로드합니다.
     */
    private fun loadStats() {
        synchronized(statsMap) {
            statsMap.clear()
            if (statsFile.exists()) {
                try {
                    val jsonText = statsFile.readText()
                    val jsonObject = JSONObject(jsonText)
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val statsObj = jsonObject.getJSONObject(key)
                        statsMap[key] = QuestionStats(
                            attemptCount = statsObj.optInt("attemptCount", 0),
                            correctCount = statsObj.optInt("correctCount", 0),
                            wrongCount = statsObj.optInt("wrongCount", 0)
                        )
                    }
                    Log.d(TAG, "🚀 Loaded ${statsMap.size} question statistics.")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to load stats: ${e.message}")
                }
            }
        }
    }

    /**
     * 통계 데이터를 로컬 디스크 파일에 영속화합니다.
     */
    private fun saveStats() {
        synchronized(statsMap) {
            try {
                val jsonObject = JSONObject()
                statsMap.forEach { (key, stats) ->
                    val statsObj = JSONObject().apply {
                        put("attemptCount", stats.attemptCount)
                        put("correctCount", stats.correctCount)
                        put("wrongCount", stats.wrongCount)
                    }
                    jsonObject.put(key, statsObj)
                }
                statsFile.writeText(jsonObject.toString())
                Log.d(TAG, "💾 Saved ${statsMap.size} question statistics to disk.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save stats: ${e.message}")
            }
        }
    }

    /**
     * 특정 문제의 퀴즈 결과를 실시간으로 기록하고 저장합니다.
     */
    suspend fun recordQuizResult(category: String, question: String, isCorrect: Boolean) = withContext(Dispatchers.IO) {
        val key = getUniqueKey(category, question)
        synchronized(statsMap) {
            val current = statsMap[key] ?: QuestionStats()
            val updated = QuestionStats(
                attemptCount = current.attemptCount + 1,
                correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
                wrongCount = if (!isCorrect) current.wrongCount + 1 else current.wrongCount
            )
            statsMap[key] = updated
            saveStats()
        }
    }

    /**
     * 특정 문제의 통계를 조회합니다.
     */
    fun getQuestionStats(category: String, question: String): QuestionStats {
        val key = getUniqueKey(category, question)
        return synchronized(statsMap) {
            statsMap[key] ?: QuestionStats()
        }
    }

    /**
     * 유저의 전체 오답 데이터 및 오답률 높은 질문 상위 N개를 조회하여 AI 프롬프트에 활용합니다.
     */
    fun getHighErrorQuestions(limit: Int = 5): List<Pair<String, Double>> {
        return synchronized(statsMap) {
            statsMap.mapNotNull { (key, stats) ->
                if (stats.attemptCount > 0) {
                    val errorRate = stats.wrongCount.toDouble() / stats.attemptCount.toDouble()
                    key to errorRate
                } else null
            }
            .sortedByDescending { it.second }
            .take(limit)
        }
    }
}
