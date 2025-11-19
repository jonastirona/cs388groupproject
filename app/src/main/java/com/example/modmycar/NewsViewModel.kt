package com.example.modmycar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.modmycar.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the news feed with personalization features.
 * Uses NewsAPI.org - a proper REST API with JSON responses and API key authentication.
 * 
 * Personalization features:
 * - Category-based filtering (technology, business, etc.)
 * - User interest-based queries (car mods, tuning, customization)
 * - Infinite scroll doomscrolling experience
 */
class NewsViewModel(
    private val repository: NewsApiRepository? = try {
        val apiKey = BuildConfig.NEWS_API_KEY
        if (apiKey.isBlank()) {
            Log.w("NewsViewModel", "NewsAPI key not found. Falling back to RSS feeds.")
            null
        } else {
            NewsApiRepository(apiKey)
        }
    } catch (e: Exception) {
        Log.e("NewsViewModel", "Failed to initialize NewsAPI repository", e)
        null
    },
    // Fallback to RSS if NewsAPI is not available
    private val fallbackRepository: RssFeedRepository = RssFeedRepository()
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
    private var currentPage = 1
    private var loading = false
    private var endReached = false

    // Personalization: User interests for car mod community
    private val userInterests = listOf(
        "car modification",
        "automotive tuning",
        "car customization",
        "aftermarket parts",
        "car performance"
    )

    init {
        // Load initial articles on creation
        refresh()
    }

    fun refresh() {
        if (loading) return
        currentPage = 1
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
            val result = if (repository != null) {
                // Use NewsAPI with personalization
                repository.getPersonalizedArticles(
                    userInterests = userInterests,
                    page = currentPage
                )
            } else {
                // Fallback to RSS feeds
                runCatching {
                    val offset = (currentPage - 1) * pageSize
                    fallbackRepository.getArticles(limit = pageSize, offset = offset)
                }
            }

            result.onSuccess { page ->
                if (page.isNotEmpty()) {
                    currentPage++
                    _articles.value = _articles.value + page
                } else {
                    endReached = true
                }
                // If we got fewer articles than requested, we've reached the end
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

    /**
     * Load articles by category (for future personalization features)
     */
    fun loadByCategory(category: String) {
        if (repository == null || loading) return
        currentPage = 1
        endReached = false
        _articles.value = emptyList()
        _isRefreshing.value = true
        loading = true
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.getArticles(category = category, page = currentPage)
            result.onSuccess { page ->
                if (page.isNotEmpty()) {
                    currentPage++
                    _articles.value = page
                } else {
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

