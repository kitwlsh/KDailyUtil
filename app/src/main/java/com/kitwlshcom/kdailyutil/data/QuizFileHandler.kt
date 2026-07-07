package com.kitwlshcom.kdailyutil.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.kitwlshcom.kdailyutil.data.model.QuizQuestion
import com.kitwlshcom.kdailyutil.data.model.QuizType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

class QuizFileHandler {

    companion object {
        private const val TAG = "QuizFileHandler"
        private const val SHARED_DIR_NAME = "shared_quizzes"
        private const val FILE_EXTENSION = ".kquiz"

        /**
         * 퀴즈 목록을 .kquiz 형식의 JSON 파일로 변환하여 임시 공유 폴더에 저장하고 Uri를 반환합니다.
         */
        fun exportQuizzes(
            context: Context,
            categoryName: String,
            creatorName: String,
            creatorId: String,
            quizzes: List<QuizQuestion>
        ): Uri? {
            try {
                // JSON 파일 객체 생성
                val rootJson = JSONObject().apply {
                    put("category", categoryName)
                    put("creator", creatorName)
                    put("creatorId", creatorId)
                    put("version", 1)
                    
                    val questionsArray = JSONArray()
                    quizzes.forEach { q ->
                        val qObj = JSONObject().apply {
                            put("type", q.type.name)
                            put("subCategory", q.subCategory)
                            put("question", q.question)
                            put("answer", q.answer)
                            put("explanation", q.explanation)
                            put("semanticHint", q.semanticHint ?: "")
                            put("imageUrl", q.imageUrl ?: "")
                            q.options?.let { put("options", JSONArray(it)) }
                        }
                        questionsArray.put(qObj)
                    }
                    put("questions", questionsArray)
                }

                // 공유용 임시 디렉토리 확보
                val sharedDir = File(context.cacheDir, SHARED_DIR_NAME).apply {
                    if (!exists()) mkdirs()
                }

                // 안전한 파일명 생성 (특수문자 및 공백 치환)
                val safeFileName = categoryName.replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
                val tempFile = File(sharedDir, "$safeFileName$FILE_EXTENSION")
                tempFile.writeText(rootJson.toString())

                // FileProvider를 통한 content:// URI 발급
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to export quizzes: ${e.message}", e)
                return null
            }
        }

        /**
         * 외부 .kquiz 파일(Uri)을 읽어와 QuizQuestion 리스트와 출제자 정보로 파싱합니다.
         */
        fun importQuizzes(context: Context, fileUri: Uri): ImportedQuizPackage? {
            var inputStream: InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(fileUri)
                val jsonText = inputStream?.bufferedReader().use { it?.readText() } ?: return null
                return importQuizzesFromText(jsonText)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to import quizzes: ${e.message}", e)
                return null
            } finally {
                inputStream?.close()
            }
        }

        /**
         * .kquiz JSON '텍스트'를 직접 파싱한다. (AI가 파일이 아닌 텍스트로 준 경우 붙여넣기 가져오기용)
         * 앞뒤에 코드블록(```json)이나 잡텍스트가 섞여 있어도 첫 '{'부터 마지막 '}'까지만 추출해 시도한다.
         */
        fun importQuizzesFromText(rawText: String): ImportedQuizPackage? {
            return try {
                val start = rawText.indexOf('{')
                val end = rawText.lastIndexOf('}')
                if (start < 0 || end <= start) return null
                val jsonText = rawText.substring(start, end + 1)

                val rootJson = JSONObject(jsonText)
                val category = rootJson.getString("category")
                val creatorName = rootJson.optString("creator", "익명의 출제자")
                val creatorId = rootJson.optString("creatorId", "unknown")

                val questionsArray = rootJson.getJSONArray("questions")
                val questionsList = mutableListOf<QuizQuestion>()

                for (i in 0 until questionsArray.length()) {
                    val obj = questionsArray.getJSONObject(i)
                    val optionsArray = obj.optJSONArray("options")
                    val optionsList = if (optionsArray != null) {
                        List(optionsArray.length()) { idx -> optionsArray.getString(idx) }
                    } else null

                    // 가져오는 퀴즈는 해시코드로 유니크한 가상 ID를 부여하여 충돌 방지
                    val baseQuestion = obj.getString("question")
                    val uniqueId = Math.abs((category + baseQuestion).hashCode())

                    questionsList.add(
                        QuizQuestion(
                            id = uniqueId,
                            type = QuizType.fromRaw(obj.optString("type"), optionsList != null),
                            category = category,
                            subCategory = obj.optString("subCategory", ""),
                            question = baseQuestion,
                            options = optionsList,
                            answer = obj.getString("answer"),
                            explanation = obj.getString("explanation"),
                            semanticHint = obj.optString("semanticHint", null),
                            imageUrl = obj.optString("imageUrl", null)
                        )
                    )
                }
                if (questionsList.isEmpty()) return null

                ImportedQuizPackage(
                    category = category,
                    creatorName = creatorName,
                    creatorId = creatorId,
                    questions = questionsList
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to parse quiz JSON text: ${e.message}", e)
                null
            }
        }

        /**
         * 카카오톡, 라인 등 안드로이드 네이티브 공유 시트를 트리거합니다.
         */
        fun triggerShareSheet(context: Context, fileUri: Uri, categoryName: String) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "K-Quiz: $categoryName")
                putExtra(Intent.EXTRA_TEXT, "KDailyUtil 프리미엄 배움터 퀴즈 패키지 [$categoryName]가 도착했습니다! 파일을 터치해 앱에 추가해 보세요.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "K-Quiz 패키지 공유하기").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

data class ImportedQuizPackage(
    val category: String,
    val creatorName: String,
    val creatorId: String,
    val questions: List<QuizQuestion>
)
