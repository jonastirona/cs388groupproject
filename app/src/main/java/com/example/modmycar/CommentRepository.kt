package com.example.modmycar

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

private const val TAG = "CommentRepository"

interface CommentRepository {
    suspend fun getComments(postId: String): List<Comment>
    suspend fun createComment(comment: Comment): Comment
    suspend fun deleteComment(commentId: String)
    suspend fun updateComment(commentId: String, content: String): Comment
}

class SupabaseCommentRepository(
    private val client: SupabaseClient
) : CommentRepository {

    override suspend fun getComments(postId: String): List<Comment> {
        return try {
            val response = client.from("comments")
                .select {
                    filter { eq("post_id", postId) }
                    order("created_at", Order.ASCENDING)
                }
            response.decodeList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments for postId=$postId", e)
            emptyList()
        }
    }

    override suspend fun createComment(comment: Comment): Comment {
        return try {
            val data = mapOf(
                "post_id" to comment.postId,
                "user_id" to comment.userId,
                "content" to comment.content
            )
            val inserted = client.from("comments").insert(data) { select() }
            inserted.decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create comment: $comment", e)
            throw e
        }
    }

    override suspend fun deleteComment(commentId: String) {
        try {
            client.from("comments").delete {
                filter { eq("id", commentId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment id=$commentId", e)
            throw e
        }
    }

    override suspend fun updateComment(commentId: String, content: String): Comment {
        return try {
            val updated = client.from("comments").update(mapOf("content" to content)) {
                filter { eq("id", commentId) }
                select()
            }
            updated.decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update comment id=$commentId", e)
            throw e
        }
    }
}
