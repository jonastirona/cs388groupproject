package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: PostRepository =
        try {
            SupabasePostRepository(SupabaseClient.client)
        } catch (e: Exception) {
            LocalPostRepository() // fallback for offline / testing
        },
    private val userId: String? = null // If provided, only show posts from this user
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val pageSize = 20
    private var nextOffset = 0
    private var requestInFlight = false
    private var endReached = false

    // Public API
    fun refresh() {
        if (requestInFlight) return
        nextOffset = 0
        endReached = false
        _posts.value = emptyList()
        _isRefreshing.value = true
        loadNextPage()
    }

    fun loadNextPage() {
        if (requestInFlight || endReached) return
        requestInFlight = true
        _isLoading.value = true

        viewModelScope.launch {
            val result = fetchPostsPage(limit = pageSize, offset = nextOffset)
            result.onSuccess { page ->
                if (page.isEmpty() && nextOffset == 0) {
                    _posts.value = emptyList()
                } else if (page.isNotEmpty()) {
                nextOffset += page.size
                _posts.value = _posts.value + page
            }

                if (page.size < pageSize) {
                    endReached = true
                }
            }.onFailure { throwable ->
                _error.value = throwable.message ?: "Failed to load posts."
            }

            requestInFlight = false
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun fetchPostsPage(limit: Int, offset: Int) =
        runCatching {
            if (userId != null) {
                repository.getPostsByUserId(userId, limit, offset)
            } else {
                repository.getFeed(limit, offset)
            }
        }
}