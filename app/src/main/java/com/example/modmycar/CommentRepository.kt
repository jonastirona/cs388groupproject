package com.example.modmycar

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
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
                .select(columns = Columns.raw("*, profiles(id, username, display_name)")) {
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
            val inserted = client.from("comments").insert(data) {
                select(Columns.raw("*, profiles(id, username, display_name)"))
            }
                adjustPostCommentCount(comment.postId, 1)
                inserted.decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create comment: $comment", e)
            throw e
        }
    }

    override suspend fun deleteComment(commentId: String) {
        try {
            val deleted = client.from("comments").delete {
                filter { eq("id", commentId) }
                select()
            }
            val removed = deleted.decodeSingleOrNull<Comment>()
            removed?.let { adjustPostCommentCount(it.postId, -1) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment id=$commentId", e)
            throw e
        }
    }

    override suspend fun updateComment(commentId: String, content: String): Comment {
        return try {
            val updated = client.from("comments").update(mapOf("content" to content)) {
                filter { eq("id", commentId) }
                select(Columns.raw("*, profiles(id, username, display_name)"))
            }
            updated.decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update comment id=$commentId", e)
            throw e
        }
    }

    private suspend fun adjustPostCommentCount(postId: String, delta: Int) {
        try {
            val commentResult = client.from("comments").select(columns = Columns.list("id")) {
                filter { eq("post_id", postId) }
                count(Count.EXACT)
            }
            val updated = commentResult.countOrNull()?.toInt()
                ?: commentResult.decodeList<Comment>().size
            client.from("posts").update(mapOf("comments_count" to updated)) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust comments count for postId=$postId", e)
        }
    }
}
