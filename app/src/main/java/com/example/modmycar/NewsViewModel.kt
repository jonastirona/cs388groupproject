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

    private val pageSize = 20
    private var nextOffset = 0
    private var loading = false
    private var endReached = false

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
            val result = runCatching { repository.getArticles(limit = pageSize, offset = nextOffset) }
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

