package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(
    private val commentRepository: CommentRepository = SupabaseCommentRepository(SupabaseClient.client),
    private val postRepository: PostRepository = SupabasePostRepository(SupabaseClient.client)
) : ViewModel() {

    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadCommentsForPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val commentsList = commentRepository.getCommentsForPost(postId)
                _comments.value = _comments.value.toMutableMap().apply {
                    put(postId, commentsList)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load comments: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addComment(postId: String, userId: String, content: String) {
        android.util.Log.d("CommentViewModel", "addComment called: postId=$postId, userId=$userId, content=$content")
        if (content.isBlank()) {
            _error.value = "Comment cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("CommentViewModel", "Creating comment object...")
                val newComment = Comment(
                    postId = postId,
                    userId = userId,
                    content = content.trim()
                    // createdAt will be set by Supabase automatically
                )
                android.util.Log.d("CommentViewModel", "Calling commentRepository.createComment...")
                val createdComment = commentRepository.createComment(newComment)
                android.util.Log.d("CommentViewModel", "Comment created successfully: ${createdComment.id}")
                
                // Update local state
                val currentComments = _comments.value[postId] ?: emptyList()
                _comments.value = _comments.value.toMutableMap().apply {
                    put(postId, currentComments + createdComment)
                }
                android.util.Log.d("CommentViewModel", "Updated local comments state. Total comments for post: ${_comments.value[postId]?.size}")

                // Update post comments count
                try {
                    val post = postRepository.getPost(postId)
                    val updatedPost = post.copy(commentsCount = post.commentsCount + 1)
                    postRepository.updatePost(postId, updatedPost)
                    android.util.Log.d("CommentViewModel", "Updated post comments count to ${updatedPost.commentsCount}")
                } catch (e: Exception) {
                    // Log but don't fail the comment creation
                    android.util.Log.e("CommentViewModel", "Failed to update post comments count", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("CommentViewModel", "Error posting comment", e)
                _error.value = "Failed to post comment: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                commentRepository.deleteComment(commentId)
                
                // Update local state
                val currentComments = _comments.value[postId] ?: emptyList()
                _comments.value = _comments.value.toMutableMap().apply {
                    put(postId, currentComments.filter { it.id != commentId })
                }

                // Update post comments count
                try {
                    val post = postRepository.getPost(postId)
                    val updatedPost = post.copy(commentsCount = (post.commentsCount - 1).coerceAtLeast(0))
                    postRepository.updatePost(postId, updatedPost)
                } catch (e: Exception) {
                    android.util.Log.e("CommentViewModel", "Failed to update post comments count", e)
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete comment: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

