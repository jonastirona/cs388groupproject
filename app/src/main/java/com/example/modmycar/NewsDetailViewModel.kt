package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class NewsDetailViewModel : ViewModel() {

    private val _article = MutableStateFlow<NewsArticleDetail?>(null)
    val article: StateFlow<NewsArticleDetail?> = _article.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadArticle(
        url: String,
        titleHint: String?,
        imageHint: String?,
        source: String?,
        publishedAt: String?
    ) {
        if (_isLoading.value) return
        _isLoading.value = true

        viewModelScope.launch {
            val result = runCatching {
                fetchArticle(url, titleHint, imageHint, source, publishedAt)
            }

            result.onSuccess {
                _article.value = it
            }.onFailure {
                _error.value = it.message ?: "Failed to load article."
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun fetchArticle(
        url: String,
        titleHint: String?,
        imageHint: String?,
        source: String?,
        publishedAt: String?
    ): NewsArticleDetail = withContext(Dispatchers.IO) {
        val document = Jsoup.connect(url).get()

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")
            ?: document.title()
            ?: titleHint

        val author = document.selectFirst("meta[name=author]")?.attr("content")
            ?: document.selectFirst("meta[property=article:author]")?.attr("content")

        val imageUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?: imageHint

        val published = document.selectFirst("meta[property=article:published_time]")?.attr("content")
            ?: publishedAt

        val articleElement = document.selectFirst("article") ?: document.body()
        val content = articleElement.select("p")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
            .ifBlank { articleElement.text().ifBlank { "Unable to parse article content." } }

        NewsArticleDetail(
            title = title ?: "Article",
            author = author,
            content = content,
            imageUrl = imageUrl,
            publishedAt = published,
            source = source
        )
    }
}

