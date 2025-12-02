package com.example.modmycar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository = try {
        SupabasePostRepository(SupabaseClient.client)
    } catch (e: Exception) {
        LocalPostRepository()
    },
    private val commentRepository: CommentRepository = SupabaseCommentRepository(SupabaseClient.client),
    private val likeRepository: LikeRepository = SupabaseLikeRepository(SupabaseClient.client),
    private val postStorageService: PostStorageService = SupabasePostStorageService()
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _postDeleted = MutableStateFlow(false)
    val postDeleted: StateFlow<Boolean> = _postDeleted.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val loadedPost = postRepository.getPost(postId)
                _post.value = loadedPost
                loadComments(postId)
                currentUserId?.let { userId ->
                    _isLiked.value = likeRepository.isLiked(postId, userId)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadComments(postId: String) {
        try {
            val loadedComments = commentRepository.getComments(postId)
            _comments.value = loadedComments
        } catch (e: Exception) {
            _error.value = "Failed to load comments: ${e.message}"
        }
    }

    fun toggleLike() {
        val postId = _post.value?.id ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                if (_isLiked.value) {
                    likeRepository.unlikePost(postId, userId)
                    _isLiked.value = false
                    val updatedCount = ((_post.value?.likesCount ?: 0) - 1).coerceAtLeast(0)
                    _post.value = _post.value?.copy(likesCount = updatedCount)
                } else {
                    likeRepository.likePost(postId, userId)
                    _isLiked.value = true
                    val updatedCount = (_post.value?.likesCount ?: 0) + 1
                    _post.value = _post.value?.copy(likesCount = updatedCount)
                }
                refreshPostState(postId)
            } catch (e: Exception) {
                _error.value = "Failed to toggle like: ${e.message}"
            }
        }
    }

    fun addComment(content: String) {
        val postId = _post.value?.id ?: return
        val userId = currentUserId ?: return

        if (content.isBlank()) {
            _error.value = "Comment cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val comment = Comment(
                    id = "",
                    postId = postId,
                    userId = userId,
                    content = content,
                    createdAt = ""
                )
                val created = commentRepository.createComment(comment)
                val updatedComments = _comments.value + created
                _comments.value = updatedComments
                _post.value = _post.value?.copy(commentsCount = updatedComments.size)
                refreshPostState(postId)
            } catch (e: Exception) {
                _error.value = "Failed to add comment: ${e.message}"
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                commentRepository.deleteComment(commentId)
                val updatedComments = _comments.value.filter { it.id != commentId }
                _comments.value = updatedComments
                _post.value = _post.value?.copy(commentsCount = updatedComments.size)
                _post.value?.id?.let { refreshPostState(it) }
            } catch (e: Exception) {
                _error.value = "Failed to delete comment: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deletePost() {
        val post = _post.value ?: return
        val userId = currentUserId ?: return

        // Verify user owns this post
        if (post.userId != userId) {
            _error.value = "You can only delete your own posts"
            return
        }

        viewModelScope.launch {
            _isDeleting.value = true
            try {
                // Delete media files from storage
                val mediaFileNames = post.media.mapIndexed { index, media ->
                    val extension = when {
                        media.url.contains(".jpg", ignoreCase = true) -> "jpg"
                        media.url.contains(".jpeg", ignoreCase = true) -> "jpg"
                        media.url.contains(".png", ignoreCase = true) -> "png"
                        media.url.contains(".mp4", ignoreCase = true) -> "mp4"
                        media.url.contains(".mov", ignoreCase = true) -> "mov"
                        media.url.contains(".webm", ignoreCase = true) -> "webm"
                        else -> "bin"
                    }
                    "${media.type}_${index + 1}.$extension"
                }

                if (mediaFileNames.isNotEmpty()) {
                    postStorageService.deleteAllPostMedia(userId, post.id, mediaFileNames)
                }

                // Delete post from database
                postRepository.deletePost(post.id)

                _postDeleted.value = true
            } catch (e: Exception) {
                _error.value = "Failed to delete post: ${e.message}"
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun isCurrentUserPostAuthor(): Boolean {
        val post = _post.value ?: return false
        val userId = currentUserId ?: return false
        return post.userId == userId
    }

    private suspend fun refreshPostState(postId: String) {
        try {
            val latest = postRepository.getPost(postId)
            _post.value = latest
        } catch (e: Exception) {
            Log.w("PostDetailViewModel", "Failed to refresh post $postId", e)
        }
    }
}


