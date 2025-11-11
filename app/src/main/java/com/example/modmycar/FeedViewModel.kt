package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: PostRepository =
        try {
            SupabasePostRepository(SupabaseClient.client)
        } catch (e: Exception) {
            LocalPostRepository() // fallback for offline / testing
        }
) : ViewModel() {

    private val rssRepo = RssFeedRepository()

    private suspend fun fetchCombinedPage(limit: Int, offset: Int): List<Post> {
        val supabase = runCatching { repository.getFeed(limit, offset) }.getOrElse { emptyList() }
        val rss      = runCatching { rssRepo.getRssPosts(limit, offset) }.getOrElse { emptyList() }

        return (supabase + rss)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
    }

    // ----- UI state -----
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val pageSize = 20
    private var nextOffset = 0
    private var isLoading = false
    private var endReached = false

    // Public API
    fun refresh() {
        nextOffset = 0
        endReached = false
        _posts.value = emptyList()
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || endReached) return
        isLoading = true

        viewModelScope.launch {
            val page = fetchCombinedPage(limit = pageSize, offset = nextOffset)
            if (page.isEmpty()) {
                endReached = true
            } else {
                nextOffset += page.size
                _posts.value = _posts.value + page
            }
            isLoading = false
        }
    }
}