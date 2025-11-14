package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

interface CommentRepository {
    suspend fun getCommentsForPost(postId: String): List<Comment>
    suspend fun createComment(comment: Comment): Comment
    suspend fun deleteComment(commentId: String)
    suspend fun updateComment(commentId: String, content: String): Comment
}

class SupabaseCommentRepository(
    private val client: SupabaseClient
) : CommentRepository {

    override suspend fun getCommentsForPost(postId: String): List<Comment> {
        return client.from("comments")
            .select {
                filter {
                    eq("post_id", postId)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Comment>()
    }

    override suspend fun createComment(comment: Comment): Comment {
        val inserted = client.from("comments").insert(comment) {
            select()
        }
        return inserted.decodeSingle<Comment>()
    }

    override suspend fun deleteComment(commentId: String) {
        client.from("comments").delete {
            filter { eq("id", commentId) }
        }
    }

    override suspend fun updateComment(commentId: String, content: String): Comment {
        val updated = client.from("comments").update(mapOf("content" to content)) {
            filter { eq("id", commentId) }
            select()
        }
        return updated.decodeSingle<Comment>()
    }
}


