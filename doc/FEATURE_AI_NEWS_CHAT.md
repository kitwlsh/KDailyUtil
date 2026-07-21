# 💬 뉴스 AI 대화창 (AI News Chat) — 기능 설계서 v1.0

> 뉴스탭 **'AI' 카테고리**의 기존 "1회성 맞춤 분석"에 **멀티턴 대화(채팅)** 를 얹는 기능 설계.
> 오늘 수집된 뉴스(제목+RSS 스니펫, 제한매체 제외)를 컨텍스트로 **AI에게 이어서 물어보는** 대화형 비서 + **음성 입력(STT)·답변 낭독(TTS)**.
> 작성: 2026-07-21 · 상태: **✅ 구현 완료(2026-07-21, 미배포)** — v1.4 게시 확정 후 다음 배포(vc5/v1.5)에 포함.

> ### 구현 메모 (2026-07-21)
> - **상태 소유**: 신규 VM 대신 **`BriefingViewModel` 확장**(이미 newsRepository·geminiApiKey·selectedAiCommand·기존 `SttManager`/`TtsManager`를 모두 보유, MainScreen에서 생성돼 탭 이동에도 유지). §8-1 결정.
> - **첫 답 통일**: 기존 맞춤 분석 결과를 **대화 첫 AI 말풍선으로 재활용**(자연스러운 이어가기). §8-3 결정.
> - **Chat 세션 지연 생성**: 컨텍스트(필터링된 오늘 뉴스+기존 대화) 주입은 **첫 사용자 메시지 전송 시** 생성해 불필요한 토큰/네트워크 절약.
> - **음성**: 기존 `SttManager`(ko-KR, 이미 RECORD_AUDIO 보유) 재사용 + `TtsManager.speak(playBgm=false)`로 답변 낭독.
> - **구현 파일**: [GeminiManager.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/remote/GeminiManager.kt)(`startNewsChat`/`sendChatMessage`), [AiChatSession.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/model/AiChatSession.kt), [AiChatRepository.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/repository/AiChatRepository.kt)(30일 purge·삭제), [BriefingViewModel.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/viewmodel/BriefingViewModel.kt)(대화 상태·로직), [NewsBriefingScreen.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/NewsBriefingScreen.kt)(`AiChatSection`·`ChatBubble`·`ChatHistoryDialog`·`SessionViewDialog`).
> - **잔여(선택)**: §8-4 핸즈프리 자동 낭독(현재 수동 🔊), §8-5 히스토리 토큰 상한(장기 대화 시 압축).

---

## 0. 배경 / 목표

- 현재 뉴스탭 'AI' 탭은 등록한 명령어 1개에 대해 **한 번 요약**하고 끝난다([BriefingViewModel.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/viewmodel/BriefingViewModel.kt) `generateAiCustomBriefing`, [GeminiManager.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/data/remote/GeminiManager.kt) `processAiCustomBriefing`). 후속 질문·되묻기가 안 된다.
- **목표**: 분석 결과 아래에 대화창을 두어, 사용자가 "그중 반도체만 더 자세히", "이거 왜 중요해?" 처럼 **이어서 대화**. 크롬의 '제미나이에 물어보기'와 유사한 UX를, **우리 저작권 정책 안에서** 구현.
- **음성**: 운전 중/출근길 사용 맥락에 맞춰 **말로 묻고(STT) → 답을 읽어주는(TTS)** 루프 지원.

---

## 1. 저작권 / 정책 가이드 (필독 — 기존 정책 그대로 승계)

> ⚠️ 이 기능은 **새 저작권 리스크를 만들지 않는다**. 위험은 "채팅이냐"가 아니라 **"AI에 어떤 뉴스 텍스트를 넣느냐"** 로만 결정된다. (상세 근거: [[news-ai-copyright-policy]], DEVELOPER_GUIDE "뉴스 저작권 정책")

**해도 되는 것 (이 설계의 마지노선)**
- 컨텍스트 = **제목 + `description`(RSS 스니펫)만**. 지금 `processAiCustomBriefing`이 넣는 것과 동일 범위.
- `getTopNews(20)`에 **기존 'AI 이용 금지' 필터**를 그대로 적용(`detectAiRestrictionNotice` + `isAiRestrictedDomain`).
- AI가 스니펫을 **요약·해설·변형**해 답변.

**하면 안 되는 것**
- ❌ **기사 본문 전문을 긁어(WebView innerText/`fetchFullContent`) AI에 주입** — 본문 비스크랩·제3자 전송 금지 위반. (= 진짜 크롬 방식. 공개앱에서 배제.)
- ❌ 제한매체(`aiRestricted`) 스니펫을 컨텍스트에 포함.
- ❌ AI가 특정 기사 스니펫을 **장문 그대로 반복 복붙** — 프롬프트로 요약/변형 지시 유지.

**핵심 정리**
- "본문 없이 제목+스니펫만으로 답하므로, AI가 세부 사실을 **지어낼(할루시네이션)** 수 있다" → **면책 고지 필수**, 답변에 "원문 보기" 유도.
- 사용자 본인이 직접 입력·붙여넣은 텍스트에 대한 질문은 사적복제 범위(향후 확장 여지, 이번 범위 아님).

---

## 2. UX / 화면 배치

**위치**: 뉴스탭 > 'AI' 카테고리. 기존 분석 결과 카드(`source == "Gemini AI"`) 하단에 **대화 영역**을 붙인다([NewsBriefingScreen.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/ui/screens/NewsBriefingScreen.kt) `isAiItem` 렌더 지점).

```
┌ [AI] 탭 ────────────────────────────┐
│ 서브탭: [명령어1][명령어2][＋]        │  ← 기존 유지
│ ┌ ✨ AI 맞춤 분석: "..." ───────────┐ │  ← 기존 결과 카드(= 대화 1번째 답)
│ │ (요약 본문)          [🔄재분석]  │ │
│ └──────────────────────────────────┘ │
│ ── 대화 ──────────────────────────── │  ← 신규
│  🙋 사용자: 반도체만 더 자세히         │
│  🤖 AI: ...                          │
│  🙋 사용자: (음성) ...                │
│  🤖 AI: ...                    [🔊]  │
│ ┌───────────────────────────┬──┬──┐ │
│ │ 메시지 입력…               │🎤│▶ │ │  ← 텍스트 + 음성입력 + 전송
│ └───────────────────────────┴──┴──┘ │
└──────────────────────────────────────┘
```

- **첫 답변 = 기존 맞춤 분석**을 대화 히스토리의 첫 AI 턴으로 승격(자연스러운 이어가기).
- 명령어(서브탭) 전환 시 **대화 세션 초기화**(명령어별 독립 세션).
- 각 AI 답변 말풍선에 **🔊 낭독 버튼**(TtsManager). 입력창에 **🎤 음성 입력**.
- 저작권/AI 한계 **면책 배너** 1회 노출(하단 작은 글씨 상시 or 최초 다이얼로그).
- **부적절 응답 신고** 진입점(⋮ 또는 배너 링크) — Google Play 생성형 AI 정책 대응(§7).

---

## 3. 데이터 흐름 & 컨텍스트 주입 규격 (ChatSession)

Gemini SDK(`com.google.ai.client.generativeai` 0.9.0)의 멀티턴 API 사용:
`GenerativeModel.startChat(history)` → `Chat.sendMessage(text)` → `Chat.history` 보존.

**`GeminiManager`에 추가(안)**:
```kotlin
// 뉴스 컨텍스트를 심은 대화 세션 생성. referenceNews는 호출부에서 이미 제한매체 필터링된 목록.
fun startNewsChat(command: String, referenceNews: List<NewsItem>): Chat? {
    val model = generativeModel ?: return null
    val context = buildString {
        append("당신은 개인 뉴스 비서입니다. 아래 '오늘의 뉴스 요약 목록'만 근거로 대화하세요.\n")
        append("• 목록에 없는 세부 사실은 추측하지 말고 '원문 확인이 필요하다'고 안내하세요.\n")
        append("• 기사 요약을 길게 그대로 옮기지 말고 사용자 질문에 맞춰 짧게 정리·해설하세요.\n")
        append("• 친절한 대화체, 핵심 위주.\n\n")
        append("사용자 관심(초기 명령): \"$command\"\n\n뉴스 목록(제목: 스니펫):\n")
        append(referenceNews.joinToString("\n") { "- ${it.title}: ${it.description}" })
    }
    // history 첫 턴에 컨텍스트를 user 롤로 심고, model 롤로 "이해했다"를 넣어 고정
    return model.startChat(history = listOf(
        content(role = "user") { text(context) },
        content(role = "model") { text("네, 위 뉴스 목록을 바탕으로 답변하겠습니다.") }
    ))
}
```
- 후속 발화는 `chat.sendMessage(userText)`. 세션이 히스토리를 들고 있어 별도 재주입 불필요.
- **컨텍스트 재료 = 지금과 동일**: `getTopNews(20).filterNot { detectAiRestrictionNotice || isAiRestrictedDomain }`. (BriefingViewModel의 기존 코드 재사용)
- **첫 답변 재활용**: 기존 `processAiCustomBriefing` 결과를 첫 AI 말풍선으로 쓰고, 이후부터 `chat`으로 이어감. (또는 첫 답도 `startNewsChat`+`sendMessage(command)`로 통일 — 구현 시 택1)

**상태(ViewModel, 안)**: `StockViewModel`처럼 Activity 스코프 유지 권장(탭 이동해도 대화 보존).
```kotlin
data class ChatMessage(val role: Role, val text: String) // Role = USER | AI
val chatMessages: StateFlow<List<ChatMessage>>
val isChatResponding: StateFlow<Boolean>
fun sendChat(userText: String)   // chat.sendMessage → 히스토리 append
fun resetChat()                  // 명령어 전환/새로고침 시
```

---

## 4. 음성 설계 (STT / TTS)

**입력(STT)**: Android `SpeechRecognizer` 또는 `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`(한국어 `ko-KR`).
- 권한: **이미 보유한 `RECORD_AUDIO`** 로 충분 — 추가 권한/정책 영향 없음.
- 인식 결과 텍스트를 입력창에 채우거나 바로 `sendChat()`.
- ⚠️ 오디오 캡처(녹음) 기능과 마이크 자원 충돌 주의 — 녹음 중이면 STT 비활성/안내.

**출력(TTS)**: 기존 [TtsManager.kt](../app/src/main/java/com/kitwlshcom/kdailyutil/audio/TtsManager.kt) 재사용. AI 답변 말풍선의 🔊 버튼 → `ttsManager.speak(answer)`.
- (선택) "핸즈프리 모드": 음성 질문 → 답변 자동 낭독 → 다시 듣기 대기. 운전 맥락. **2차 확장으로 권장**(초기엔 수동 버튼).

> 음성은 **저작권과 무관** — 컨텍스트 규격(§1·§3)만 지키면 텍스트와 동일하게 안전.

---

## 5. 대화 수명 · 보관 정책 (핵심 결정, 2026-07-21)

> 브리핑은 **매일 갱신**되는데 대화는 이어지려는 성질이라 둘의 수명이 다르다. "대화를 날짜 넘어 무한히 이어붙이기"는 ① 컨텍스트 불일치(어제 뉴스에 묶인 세션에 오늘 질문 → 오답) ② 토큰 폭증(429) ③ 날짜 경계 모호를 유발하므로 **금지**. 대신 아래 세션 모델을 따른다.

**원칙: 세션 = 하나의 브리핑**
- 새 브리핑 생성(**날짜가 바뀌거나 🔄 재분석**)마다 **새 대화 세션** 시작 → 답변은 항상 "그 시점 뉴스"에 근거.
- 세션 키 = **(명령어 + 브리핑 날짜)**. 명령어별·날짜별로 독립된 대화.
- "매일 새 대화의 시작"이 맞다. 단, 전날 대화는 **버리지 않고 기록으로 남긴다**(아래).

**보관 (결정: 기록 보관 + 자동정리 30일 + 사용자 수동 삭제)**
- 지난 세션은 삭제하지 않고 **'대화 기록'에 보관** → "전에 한 대화를 다시 보고 싶다" 요구 충족.
- 지난 세션 열람은 **읽기 전용**(스크롤·복사 O, 추가 질문 ✕). 스테일 컨텍스트에 새 질문을 붙이면 오답이 나므로, 이어가고 싶으면 **오늘 세션에서 다시 질문**하도록 유도.
- **자동정리**: 마지막 활동 후 **30일** 초과 세션은 자동 삭제(purge). 앱 시작/기록 진입 시 검사. (기존 공시 90일 TTL 캐시와 동일 철학, 기간만 30일)
- **수동 삭제(사용자 선택)**: 사용자가 기록에서 **개별 세션 삭제** + **전체 지우기** 가능. 자동 30일 도래 전이라도 즉시 삭제할 수 있다.

**저장 규격**
- 로컬 `filesDir`에 JSON으로만 저장(외부 전송·공유 없음) → **사적 이용 범위라 저작권 무관**. 기존 `quizzes_v2.json`·AI 분석 파일 캐시와 동일 패턴.
- 파일(안): `filesDir/ai_chats/{cmdHash}_{yyyyMMdd}.json` (세션당 1파일) 또는 단일 `ai_chat_history.json`(세션 리스트). 세션 = `{ key, command, date, messages[], lastActiveAt }`.
- (선택) 민감정보는 아니지만 일관성 위해 DataStore 백업 제외 규칙처럼 백업 포함 여부 검토.

**UX (대화 기록)**
```
[AI 탭] 오늘 브리핑 + 대화창(활성)
  상단 [🕓 대화 기록] 진입점
    └ 07-21 "반도체 이슈"   (오늘·이어쓰기 가능)     [🗑]
      07-20 "반도체 이슈"   (읽기전용)               [🗑]
      07-19 "환율 전망"     (읽기전용)               [🗑]
      …                                    [전체 지우기]
    (마지막 활동 30일 초과 세션은 자동 삭제)
```

---

## 6. 구현 작업 항목 (파일별)

| 파일 | 작업 |
|---|---|
| `data/remote/GeminiManager.kt` | `startNewsChat()` 추가, `Chat` 세션 보관/`sendMessage` 래핑 |
| `data/repository/…` (신규 `AiChatRepository` 안) | 대화 세션 JSON 저장/로드, 30일 자동정리(purge), 개별·전체 수동 삭제 |
| `ui/viewmodel/BriefingViewModel.kt` (또는 신규 `NewsChatViewModel`) | `chatMessages`/`isChatResponding`/`chatSessions` 상태, `sendChat`/`resetChat`, referenceNews 필터 재사용, 세션 키=(명령어+날짜) |
| `ui/screens/NewsBriefingScreen.kt` | AI 카드 하단 대화 영역(말풍선 리스트 + 입력창 + 🎤/▶/🔊), 🕓 대화 기록 화면(읽기전용·삭제), 면책 배너, 신고 진입점 |
| 신규 `ui/components/…` (선택) | STT 런처 헬퍼, ChatBubble 컴포넌트 |
| 문서 | 본 문서 + README 인덱스 등록(완료), 구현 후 DEVELOPER_GUIDE '지금 상태'·RELEASE_NOTES 반영 |

---

## 7. Google Play 생성형 AI 정책 대응 (배포 전 필수)

Google Play **"AI-Generated Content" 정책**은 사용자와 상호작용하는 생성형 AI에 대해 요구:
- [ ] **부적절한 AI 응답 신고 수단** 제공(대화창 내 신고 링크/메뉴 → 기존 문의 메일 재사용 가능).
- [ ] AI 생성물임을 사용자가 알 수 있게 표시(말풍선 `🤖`/"AI" 라벨 + 이미 있는 'Gemini AI 기술 활용 고지').
- [ ] 답변 정확성 한계 **면책 고지**(할루시네이션 가능, 원문 확인 권장).
- [x] 저작권: 본문 비스크랩·제한매체 필터 승계(§1).

---

## 8. 미해결 / 결정 필요

1. ~~**상태 소유**~~ ✅ **결정됨**: `BriefingViewModel` 확장(의존성 재사용 + 탭 이동 보존).
2. ~~**대화 영속화**~~ ✅ **결정됨(§5)**: 로컬 파일 보관 + 30일 자동정리 + 사용자 수동 삭제.
3. ~~**첫 답 통일 여부**~~ ✅ **결정됨**: 기존 분석 결과를 대화 첫 AI 말풍선으로 재활용.
4. **핸즈프리 자동 낭독**: 1차 제외(수동 🔊) → 2차 확장(미구현).
5. **토큰/할당량**: 대화가 길어지면 히스토리 누적으로 토큰↑(무료 플랜 429 위험). 히스토리 길이 상한 or 오래된 턴 요약 압축 고려(미구현).

---

## 9. 버전 / 배포 영향

- 구현 시 **다음 배포 = versionCode 5 / versionName 1.5**(스킴: versionName 끝자리=versionCode). 현재 v1.4는 출시 검토중이므로 **v1.4 게시 확정 후 착수** 권장.
- 배포 전 §7 체크리스트 완료 + 실기기 음성 인식/낭독 점검 필수.
