package com.example.modmycar

data class NewsArticleSummary(
    val id: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val source: String?,
    val publishedAt: String?,
    val link: String
)

data class NewsArticleDetail(
    val title: String,
    val author: String?,
    val content: String,
    val imageUrl: String?,
    val publishedAt: String?,
    val source: String?
)

