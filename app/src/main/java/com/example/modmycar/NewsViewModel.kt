package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    private val repository: NewsApiRepository = NewsApiRepository()
) : ViewModel() {

    private val _articles = MutableStateFlow<List<NewsArticleSummary>>(emptyList())
    val articles: StateFlow<List<NewsArticleSummary>> = _articles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Raw user interest keywords derived from their posts, cars, mods, and liked posts.
     *
     * The screen is responsible
     * for collecting and updating this list. The news layer then uses it to personalize queries.
     */
    var userKeywords: List<String> = emptyList()
        private set

    private val pageSize = 100
    private var nextOffset = 0
    private var loading = false
    private var endReached = false

    /**
     * Updates the current user interest keywords that will be used for subsequent news requests.
     *
     * Calling this does not automatically refresh; callers can explicitly call [refresh] if they
     * want to immediately reload the feed with the new interests.
     */
    fun setUserKeywords(keywords: List<String>) {
        userKeywords = keywords
    }

    fun refresh() {
        if (loading) return
        nextOffset = 0
        endReached = false
        _articles.value = emptyList()
        _isRefreshing.value = true
        loadNextPage()
    }

    fun loadNextPage() {
        if (loading || endReached) return
        loading = true
        _isLoading.value = true

        viewModelScope.launch {
            val result = runCatching {
                repository.getArticles(
                    limit = pageSize,
                    offset = nextOffset,
                    userKeywords = userKeywords
                )
            }
            result.onSuccess { page ->
                if (page.isNotEmpty()) {
                    nextOffset += page.size
                    _articles.value = _articles.value + page
                }
                if (page.size < pageSize) {
                    endReached = true
                }
            }.onFailure {
                _error.value = it.message ?: "Failed to load news."
            }

            loading = false
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

