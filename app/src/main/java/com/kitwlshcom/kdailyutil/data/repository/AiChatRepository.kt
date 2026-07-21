package com.kitwlshcom.kdailyutil.data.repository

import android.content.Context
import android.util.Log
import com.kitwlshcom.kdailyutil.data.model.AiChatSession
import com.kitwlshcom.kdailyutil.data.model.ChatMessage
import com.kitwlshcom.kdailyutil.data.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 뉴스 AI 대화 세션의 로컬 영속화 저장소.
 * 정책(doc/FEATURE_AI_NEWS_CHAT.md §5):
 *  - 로컬 filesDir/ai_chats/{key}.json 에만 저장(외부 전송/공유 없음 → 사적 이용, 저작권 무관).
 *  - 마지막 활동 후 30일 초과 세션은 자동 삭제(purge).
 *  - 사용자가 개별 세션 삭제 / 전체 지우기 가능.
 */
class AiChatRepository(context: Context) {

    private val dir: File = File(context.filesDir, "ai_chats").apply { if (!exists()) mkdirs() }

    companion object {
        private const val TAG = "AiChatRepository"
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30일
    }

    /** 세션 키 생성: (명령어 해시 + 날짜). 파일명 안전을 위해 명령어는 해시로 축약. */
    fun sessionKey(command: String, date: String): String {
        val cmdHash = Integer.toHexString(command.trim().hashCode())
        return "${cmdHash}_$date"
    }

    private fun fileFor(key: String) = File(dir, "$key.json")

    suspend fun saveSession(session: AiChatSession) = withContext(Dispatchers.IO) {
        try {
            val messagesArr = JSONArray()
            session.messages.forEach { m ->
                messagesArr.put(JSONObject().apply {
                    put("role", m.role.name)
                    put("text", m.text)
                })
            }
            val obj = JSONObject().apply {
                put("key", session.key)
                put("command", session.command)
                put("date", session.date)
                put("lastActiveAt", session.lastActiveAt)
                put("messages", messagesArr)
            }
            fileFor(session.key).writeText(obj.toString(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save chat session ${session.key}", e)
        }
    }

    suspend fun loadSession(key: String): AiChatSession? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!file.exists()) return@withContext null
        try {
            parseSession(JSONObject(file.readText(StandardCharsets.UTF_8)))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load chat session $key", e)
            null
        }
    }

    /** 만료 세션을 정리한 뒤 최신 활동순(내림차순)으로 전체 세션을 반환. */
    suspend fun loadAllSessions(): List<AiChatSession> = withContext(Dispatchers.IO) {
        purgeExpired()
        val files = dir.listFiles { f -> f.extension == "json" } ?: return@withContext emptyList()
        files.mapNotNull { f ->
            try {
                parseSession(JSONObject(f.readText(StandardCharsets.UTF_8)))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to parse ${f.name}, deleting", e)
                f.delete()
                null
            }
        }.sortedByDescending { it.lastActiveAt }
    }

    suspend fun deleteSession(key: String) = withContext(Dispatchers.IO) {
        try {
            fileFor(key).delete()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete chat session $key", e)
        }
        Unit
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            dir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear chat history", e)
        }
        Unit
    }

    /** 마지막 활동 후 RETENTION_MS 초과 세션 자동 삭제. */
    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dir.listFiles { f -> f.extension == "json" }?.forEach { f ->
            try {
                val obj = JSONObject(f.readText(StandardCharsets.UTF_8))
                val last = obj.optLong("lastActiveAt", 0L)
                if (now - last > RETENTION_MS) {
                    f.delete()
                    Log.d(TAG, "🧹 Purged expired chat session ${f.name}")
                }
            } catch (e: Exception) {
                f.delete() // 손상 파일도 정리
            }
        }
        Unit
    }

    private fun parseSession(obj: JSONObject): AiChatSession {
        val messagesArr = obj.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<ChatMessage>()
        for (i in 0 until messagesArr.length()) {
            val m = messagesArr.getJSONObject(i)
            val role = try { ChatRole.valueOf(m.optString("role", "AI")) } catch (e: Exception) { ChatRole.AI }
            messages.add(ChatMessage(role = role, text = m.optString("text", "")))
        }
        return AiChatSession(
            key = obj.optString("key", ""),
            command = obj.optString("command", ""),
            date = obj.optString("date", ""),
            messages = messages,
            lastActiveAt = obj.optLong("lastActiveAt", 0L)
        )
    }
}
