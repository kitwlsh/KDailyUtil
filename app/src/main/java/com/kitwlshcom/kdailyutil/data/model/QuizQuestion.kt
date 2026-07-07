package com.kitwlshcom.kdailyutil.data.model

enum class QuizType {
    MULTIPLE_CHOICE,
    SUBJECTIVE;

    companion object {
        /**
         * 알 수 없거나 오타 난 type 문자열(예: "MULTIPLE_CHOENCE")을 만나도 예외를 던지지 않고
         * 안전하게 변환한다. 보기(options)가 있으면 객관식, 없으면 주관식으로 폴백.
         * (예전엔 QuizType.valueOf()가 예외를 던져 문항 배치/카테고리 전체가 통째로 버려졌음)
         */
        fun fromRaw(raw: String?, hasOptions: Boolean): QuizType =
            when (raw?.trim()?.uppercase()) {
                "MULTIPLE_CHOICE" -> MULTIPLE_CHOICE
                "SUBJECTIVE" -> SUBJECTIVE
                else -> if (hasOptions) MULTIPLE_CHOICE else SUBJECTIVE
            }
    }
}

data class QuizQuestion(
    val id: Int,
    val type: QuizType,
    val category: String,
    val subCategory: String = "",
    val question: String,
    val options: List<String>? = null,
    val answer: String,
    val explanation: String,
    val semanticHint: String? = null,
    val imageUrl: String? = null
)
