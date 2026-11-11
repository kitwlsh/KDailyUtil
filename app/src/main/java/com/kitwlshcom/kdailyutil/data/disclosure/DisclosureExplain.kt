package com.kitwlshcom.kdailyutil.data.disclosure

/**
 * 🤖 공시 3줄 해설(1.7.5 · P2) — 프롬프트와 **출력 검사**. 안드로이드 API를 쓰지 않는다(단위 테스트 대상).
 *
 * 🔴 **해설은 사용자가 «🤖 3줄 해설»을 누를 때만 만든다**(2026-11-11 사용자 결정 ③).
 * 백그라운드에서 미리 만들면 키(체험 횟수)가 사용자 모르게 나간다.
 *
 * ⚠️ P2 1차는 **공시 제목·회사·갈래만** 넣는다 — 원문·주요사항보고서 정형 API는 아직 안 붙였다(§11).
 * 그래서 «무슨 일»에 숫자가 없을 수 있고, 프롬프트가 «숫자를 지어내지 말라 · 원문 확인 필요»를 못 박는다.
 */
object DisclosureExplain {

    fun prompt(corpName: String, category: DisclosureCategory, reportNm: String, rceptDt: String): String =
        """
        |당신은 한국 상장사 공시를 일반 투자자에게 쉬운 말로 풀어 주는 해설가입니다.
        |아래 공시 하나를 정확히 세 줄로 설명하세요.
        |
        |회사: $corpName
        |공시 제목: ${DisclosureCategory.displayTitle(reportNm)}
        |접수일: $rceptDt
        |분류: ${category.label}
        |
        |반드시 지킬 것:
        |- 매수·매도·보유를 권하지 말 것. 목표가, «오를 것/내릴 것» 같은 예측을 하지 말 것.
        |- 호재·악재라고 단정하지 말 것. «희석 가능성», «현금 확보 목적일 수 있음»처럼 가능성으로만 말할 것.
        |- 위에 없는 숫자(금액·비율·주식 수)를 만들지 말 것. 숫자가 필요하면 «공시 원문 확인 필요»라고 쓸 것.
        |- 이 회사에 대해 확인되지 않은 사실을 단정하지 말 것. 이 공시 유형이 일반적으로 뜻하는 바를 설명할 것.
        |
        |출력 형식(이 세 줄만, 머리말·꼬리말·마크다운 없이):
        |무슨 일: (이 공시가 알리는 내용을 한 줄로)
        |무슨 뜻: (이런 공시가 회사에 일반적으로 어떤 의미일 수 있는지 한 줄로)
        |확인할 점: (투자자가 원문에서 다음으로 확인해 볼 것 한 줄로)
        """.trimMargin()

    /** 화면에 보일 세 줄 머리말. */
    val LABELS = listOf("무슨 일:", "무슨 뜻:", "확인할 점:")

    /**
     * 모델 출력을 정리한다. 세 줄 형식을 못 맞췄거나 금지 표현이 섞였으면 **null** — 보여 주지도, 캐시하지도 않는다.
     *
     * 🔴 프롬프트가 금지해도 모델이 어길 수 있다. 이 기능은 «사실을 전한다»(§8)가 전제라,
     * 어긴 출력을 «AI가 그렇게 말했다»며 내보내는 것보다 «못 만들었다»고 하는 편이 낫다.
     */
    fun clean(raw: String): String? {
        val lines = raw.replace("**", "").lines()
            .map { it.trim().trimStart('-', '*', '•', ' ').trim() }
            .filter { it.isNotBlank() }
        val picked = LABELS.map { label -> lines.firstOrNull { it.startsWith(label) } ?: return null }
        val text = picked.joinToString("\n")
        if (violates(text)) return null
        return text
    }

    /** 투자 권유·예측 표현. 단어 하나가 아니라 «권하는 말투»를 잡는다(«매수» 자체는 «자기주식 매수»처럼 사실일 수 있다). */
    fun violates(text: String): Boolean = BANNED.any { it.containsMatchIn(text) }

    private val BANNED = listOf(
        Regex("(매수|매도|매입|보유)\\s*(하세요|하십시오|를 권|을 권|추천|하는 것이 좋|할 만)"),
        Regex("목표\\s*(주)?가"),
        Regex("(주가|가격)[가이는]?\\s*(오를|내릴|상승할|하락할)\\s*(것|전망)"),
        Regex("(급등|폭등|폭락)\\s*(예상|전망|가능성이 높)"),
        Regex("(명백한|확실한)\\s*(호재|악재)"),
        Regex("(지금이|지금은)\\s*(살|팔|매수|매도)")
    )
}
