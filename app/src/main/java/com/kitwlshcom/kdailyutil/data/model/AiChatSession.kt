package com.kitwlshcom.kdailyutil.data.model

/** 대화 말풍선의 화자 */
enum class ChatRole { USER, AI }

/** 대화 한 줄(말풍선) */
data class ChatMessage(
    val role: ChatRole,
    val text: String
)

/**
 * 뉴스 AI 대화 세션.
 * 정책(doc/FEATURE_AI_NEWS_CHAT.md §5): 세션 = 하나의 브리핑. 세션 키 = (명령어 + 브리핑 날짜).
 * 날짜가 바뀌거나 재분석하면 새 세션이 시작되고, 지난 세션은 읽기 전용으로 보관된다.
 */
data class AiChatSession(
    val key: String,            // "${cmdHash}_${yyyyMMdd}"
    val command: String,        // 초기 명령/관심사
    val date: String,           // yyyy-MM-dd (브리핑 날짜)
    val messages: List<ChatMessage>,
    val lastActiveAt: Long      // epoch millis (자동정리 기준)
)
