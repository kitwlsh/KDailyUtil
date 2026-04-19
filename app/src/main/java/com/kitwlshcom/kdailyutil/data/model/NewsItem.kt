package com.kitwlshcom.kdailyutil.data.model

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String,
    var summary: String = "" // AI 요약 결과가 들어갈 자리
)
