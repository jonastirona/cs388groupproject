package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LikeViewModel(
    private val likeRepository: LikeRepository = SupabaseLikeRepository(SupabaseClient.client),
    private val postRepository: PostRepository = SupabasePostRepository(SupabaseClient.client)
) : ViewModel() {

    // Map of postId to Set of userIds who liked it
    private val _likedPosts = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val likedPosts: StateFlow<Map<String, Set<String>>> = _likedPosts.asStateFlow()

    // Map of postId to like count
    private val _likeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likeCounts: StateFlow<Map<String, Int>> = _likeCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Checks if a post is an RSS post (not stored in the database).
     * RSS posts have IDs that start with "rss-".
     */
    private fun isRssPost(postId: String): Boolean {
        return postId.startsWith("rss-")
    }

    fun checkIfUserLikedPost(postId: String, userId: String) {
        // Skip database query for RSS posts - they're not in the database
        if (isRssPost(postId)) {
            android.util.Log.d("LikeViewModel", "Skipping like check for RSS post: $postId")
            return
        }
        
        viewModelScope.launch {
            try {
                val hasLiked = likeRepository.hasUserLikedPost(postId, userId)
                _likedPosts.value = _likedPosts.value.toMutableMap().apply {
                    val currentLikers = get(postId) ?: emptySet()
                    if (hasLiked) {
                        put(postId, currentLikers + userId)
                    } else {
                        put(postId, currentLikers - userId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LikeViewModel", "Error checking like status", e)
            }
        }
    }

    fun loadLikesForPost(postId: String) {
        // Skip database query for RSS posts - they're not in the database
        if (isRssPost(postId)) {
            android.util.Log.d("LikeViewModel", "Skipping like load for RSS post: $postId")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val likes = likeRepository.getLikesForPost(postId)
                val likerIds = likes.map { it.userId }.toSet()
                val count = likes.size

                _likedPosts.value = _likedPosts.value.toMutableMap().apply {
                    put(postId, likerIds)
                }
                _likeCounts.value = _likeCounts.value.toMutableMap().apply {
                    put(postId, count)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load likes: ${e.message}"
                android.util.Log.e("LikeViewModel", "Error loading likes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: String, userId: String) {
        // RSS posts are not stored in the database, so we can't like them
        if (isRssPost(postId)) {
            android.util.Log.d("LikeViewModel", "Cannot like RSS post: $postId")
            _error.value = "Cannot like RSS feed posts"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentLikers = _likedPosts.value[postId] ?: emptySet()
                val isLiked = currentLikers.contains(userId)

                if (isLiked) {
                    // Unlike
                    likeRepository.removeLike(postId, userId)
                    _likedPosts.value = _likedPosts.value.toMutableMap().apply {
                        put(postId, currentLikers - userId)
                    }
                    val newCount = (_likeCounts.value[postId] ?: 0) - 1
                    _likeCounts.value = _likeCounts.value.toMutableMap().apply {
                        put(postId, newCount.coerceAtLeast(0))
                    }

                    // Update post likes count
                    try {
                        val post = postRepository.getPost(postId)
                        val updatedPost = post.copy(likesCount = (post.likesCount - 1).coerceAtLeast(0))
                        postRepository.updatePost(postId, updatedPost)
                    } catch (e: Exception) {
                        android.util.Log.e("LikeViewModel", "Failed to update post likes count", e)
                    }
                } else {
                    // Like
                    likeRepository.addLike(postId, userId)
                    _likedPosts.value = _likedPosts.value.toMutableMap().apply {
                        put(postId, currentLikers + userId)
                    }
                    val newCount = (_likeCounts.value[postId] ?: 0) + 1
                    _likeCounts.value = _likeCounts.value.toMutableMap().apply {
                        put(postId, newCount)
                    }

                    // Update post likes count
                    try {
                        val post = postRepository.getPost(postId)
                        val updatedPost = post.copy(likesCount = post.likesCount + 1)
                        postRepository.updatePost(postId, updatedPost)
                    } catch (e: Exception) {
                        android.util.Log.e("LikeViewModel", "Failed to update post likes count", e)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle like: ${e.message}"
                android.util.Log.e("LikeViewModel", "Error toggling like", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isPostLikedByUser(postId: String, userId: String?): Boolean {
        if (userId == null) return false
        return _likedPosts.value[postId]?.contains(userId) ?: false
    }

    fun getLikeCount(postId: String): Int {
        return _likeCounts.value[postId] ?: 0
    }

    fun initializeLikeCount(postId: String, count: Int) {
        _likeCounts.value = _likeCounts.value.toMutableMap().apply {
            if (!containsKey(postId)) {
                put(postId, count)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

