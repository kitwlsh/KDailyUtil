package com.kitwlshcom.kdailyutil.data.model

enum class QuizType {
    MULTIPLE_CHOICE,
    SUBJECTIVE
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
