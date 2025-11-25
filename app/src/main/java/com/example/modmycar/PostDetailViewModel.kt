package com.example.modmycar

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
    private val likeRepository: LikeRepository = SupabaseLikeRepository(SupabaseClient.client)
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

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
                    // Update like count
                    _post.value = _post.value?.copy(
                        likesCount = (_post.value?.likesCount ?: 0) - 1
                    )
                } else {
                    likeRepository.likePost(postId, userId)
                    _isLiked.value = true
                    // Update like count
                    _post.value = _post.value?.copy(
                        likesCount = (_post.value?.likesCount ?: 0) + 1
                    )
                }
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
                _comments.value = _comments.value + created
                // Update comment count
                _post.value = _post.value?.copy(
                    commentsCount = (_post.value?.commentsCount ?: 0) + 1
                )
            } catch (e: Exception) {
                _error.value = "Failed to add comment: ${e.message}"
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                commentRepository.deleteComment(commentId)
                _comments.value = _comments.value.filter { it.id != commentId }
                // Update comment count
                _post.value = _post.value?.copy(
                    commentsCount = (_post.value?.commentsCount ?: 0) - 1
                )
            } catch (e: Exception) {
                _error.value = "Failed to delete comment: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}


