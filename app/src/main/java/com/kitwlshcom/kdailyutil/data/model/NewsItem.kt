package com.kitwlshcom.kdailyutil.data.model

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val source: String,
    var summary: String = "", // AI 요약 결과가 들어갈 자리
    var fullContent: String = "", // 뉴스 사이트에서 가져온 전문
    var fullContentHtml: String = "" // 뉴스 사이트에서 가져온 클린 HTML 전문
)
