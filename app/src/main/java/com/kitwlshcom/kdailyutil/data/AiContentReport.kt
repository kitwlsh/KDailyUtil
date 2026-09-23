package com.kitwlshcom.kdailyutil.data

/**
 * 「이거 이상해요」를 **앱 안에서** 우리에게 보내는 길 (2026-09-23).
 *
 * 다루는 것 = **지문 · 이해도 퀴즈 · 증시 AI 분석**. 전부 AI가 만든 글이고, 전부 같은 주소로 간다.
 *
 * 🔴 **왜 필요한가 — 두 가지다.**
 *
 * 1. **정책** — Play는 AI로 콘텐츠를 *만들어 주는* 앱에 «앱을 나가지 않고 신고할 수단»을 요구하고,
 *    그 신고를 필터링·검수에 반영하라고 한다(정책 14094294). 우리 앱은 AI 기능이 13종이고
 *    사용자 키로 실제 호출하므로 «남의 AI 결과물을 보여 주기만 하는 앱»이 아니다.
 * 2. 🔴 **실질** — 이게 없으면 이상한 지문이 나가도 **우리는 영영 모른다.** 지금 사용자가 할 수 있는
 *    것은 「숨기기」뿐인데 그것은 «내 화면에서 치우는 것»이고, 정책이 요구하는 것도 우리가 필요한
 *    것도 «**우리에게 알리는 것**»이다. 둘은 다른 기능이다.
 *    로봇 프롬프트를 고칠 **유일한 신호**가 신고다.
 *
 * ✅ **새 기능이 아니다** — 뉴스 브리핑(`NewsBriefingScreen`)과 퀴즈(`QuizScreen`)에는 이미 있다.
 * 퀴즈 것과 **같은 문법**으로 지문에 옮겨 붙인 것이다(같은 주소 · 같은 제목 꼴 · 자동으로 채워지는 식별자).
 *
 * 안드로이드 API를 쓰지 않는 순수 함수다(단위 테스트 대상). 인텐트를 띄우는 일은 화면이 한다.
 */
object AiContentReport {

    /** 받는 곳. 🔴 퀴즈·뉴스 신고와 **같은 주소**여야 한 곳에서 본다. */
    const val EMAIL = "kitwlsh@gmail.com"

    /**
     * 메일 본문에 넣는 본문 발췌 길이.
     *
     * ⚠️ 지문 전문을 넣지 않는다 — 토요일 장문은 1,200자라 메일 앱이 잘라 버리거나
     * 사용자가 «내가 뭘 보내는지» 알 수 없게 된다. **어느 글인지 알아볼 만큼**이면 된다.
     */
    const val EXCERPT_MAX = 160

    /**
     * 신고할 수 있는 글인가.
     *
     * 🔴 **내 지문([PassageSource.MINE])은 제외한다** — 사용자가 찍거나 붙여 넣은 글은
     * 우리가 고칠 대상이 아니다. 퀴즈도 «개인 제작 문제는 신고 제외»로 같은 판단을 이미 했다
     * (`QuizScreen`의 `isPersonalQuiz`). 거기에 신고 버튼을 두면 «보내도 아무 일도 안 일어나는
     * 버튼»이 되고, 그것은 없는 것만 못하다.
     */
    fun canReport(source: PassageSource): Boolean = source != PassageSource.MINE

    /**
     * 메일 제목. 🔴 **식별자를 제목에 둔다** — 받은 편지함에서 열어 보지 않고도 어느 글인지 안다
     * (퀴즈 신고가 「문제 ID: N」을 제목에 두는 것과 같다).
     *
     * @param id 로봇 지문의 id. 내장 지문처럼 id가 없으면 null → 제목으로 대신한다.
     */
    /**
     * 본문을 **한 줄로 펴서** 앞부분만 잘라 낸다.
     *
     * 🔴 줄바꿈을 그대로 두면 메일 본문에서 «자동으로 채운 정보»와 «사용자가 쓴 말»의
     * 경계가 무너져 우리가 읽을 때 어디까지가 지문인지 알 수 없다.
     * ⚠️ 정규식을 쓰지 않는다 — 공백 종류(전각 공백·줄바꿈·탭)를 한 번에 다루면서
     *    읽는 사람이 무슨 일이 일어나는지 바로 알 수 있는 쪽을 골랐다.
     */
    private fun excerptOf(text: String): String {
        val sb = StringBuilder()
        for (ch in text.trim()) {
            if (ch.isWhitespace()) {
                if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
            } else {
                sb.append(ch)
            }
        }
        val one = sb.toString()
        return if (one.length > EXCERPT_MAX) one.take(EXCERPT_MAX) + "…" else one
    }

    fun subject(id: Long?, title: String): String {
        val tag = id?.toString() ?: title.trim().ifBlank { "(제목 없음)" }
        return "[KDailyUtil 지문 신고] $tag"
    }

    /**
     * 메일 본문. **식별 정보는 우리가 채우고, 사용자는 «무엇이 이상한지»만 쓰게 한다.**
     * 빈 메일 창을 주면 대부분은 그냥 닫는다 — 신고가 안 오는 것과 같다.
     */
    fun body(id: Long?, title: String, text: String, source: PassageSource): String {
        val excerpt = excerptOf(text)
        return buildString {
            appendLine("[어떤 점이 이상한지 여기에 적어주세요]")
            appendLine("예) 문장이 어색해요 · 사실과 달라요 · 불쾌한 표현이 있어요 · 남의 글을 옮겨 온 것 같아요")
            appendLine()
            appendLine("─── 아래는 자동으로 채워진 정보예요(지우지 마세요) ───")
            appendLine("제목: ${title.ifBlank { "(제목 없음)" }}")
            if (id != null) appendLine("지문 ID: $id")
            appendLine("출처: ${source.badge}")
            appendLine("글자 수: ${text.length}자")
            appendLine("본문 일부: $excerpt")
        }
    }

    /** 화면에 뜨는 글씨. ⚠️ 작게 — 읽기를 방해하면 «안전장치»가 아니라 소음이다(출처 문구와 같은 기준). */
    const val LABEL = "🚩 이 지문 신고"

    /**
     * 증시 AI 분석 신고 (2026-09-23에 같이 붙였다).
     *
     * 🔴 **여기에 없던 것이 이번에 드러났다** — 지문에 붙이려고 화면들을 훑다가
     * 「📊 AI 실적 요약」·「🗓️ AI 사전 전망」에는 신고가 **한 곳도 없다**는 것을 확인했다.
     * 이쪽은 «사실을 말하는 글»이라 틀린 숫자가 나가면 지문보다 해가 크다.
     *
     * @param what 「삼성전자 AI 실적 요약」처럼 어느 회사의 무슨 분석인지.
     */
    fun aiSubject(what: String): String = "[KDailyUtil AI 분석 신고] ${what.trim().ifBlank { "(제목 없음)" }}"

    /** 증시 AI 분석 신고 본문 — 무엇을 보고 이상하다고 했는지가 있어야 프롬프트를 고칠 수 있다. */
    fun aiBody(what: String, content: String): String = buildString {
        appendLine("[어떤 점이 이상한지 여기에 적어주세요]")
        appendLine("예) 숫자가 실제 공시와 달라요 · 없는 사실을 지어냈어요 · 투자 권유처럼 읽혀요")
        appendLine()
        appendLine("─── 아래는 자동으로 채워진 정보예요(지우지 마세요) ───")
        appendLine("분석: ${what.ifBlank { "(제목 없음)" }}")
        appendLine("내용 일부: ${excerptOf(content)}")
    }

    /** 이해도 퀴즈(앱이 그 자리에서 AI로 만든 문제) 신고. 지문과 **다른 물건**이라 제목을 가른다. */
    fun quizSubject(): String = "[KDailyUtil 이해도 퀴즈 신고]"

    /** 이해도 퀴즈 신고 본문 — 어느 지문으로 만든 문제였는지가 유일한 단서다. */
    fun quizBody(passage: String, questions: List<String>): String {
        val excerpt = excerptOf(passage)
        return buildString {
            appendLine("[어떤 문제가 이상한지 여기에 적어주세요]")
            appendLine("예) 정답이 틀렸어요 · 지문에 없는 내용이에요 · 보기가 중복돼요")
            appendLine()
            appendLine("─── 아래는 자동으로 채워진 정보예요(지우지 마세요) ───")
            appendLine("지문 일부: $excerpt")
            questions.forEachIndexed { i, q -> appendLine("문제 ${i + 1}: $q") }
        }
    }
}
