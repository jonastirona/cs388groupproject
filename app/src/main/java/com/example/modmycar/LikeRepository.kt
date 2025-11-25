package com.example.modmycar

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

private const val LIKE_TAG = "LikeRepository"

interface LikeRepository {
    suspend fun isLiked(postId: String, userId: String): Boolean
    suspend fun likePost(postId: String, userId: String): Like
    suspend fun unlikePost(postId: String, userId: String)
    suspend fun getLikeCount(postId: String): Int
}

class SupabaseLikeRepository(
    private val client: SupabaseClient
) : LikeRepository {

    override suspend fun isLiked(postId: String, userId: String): Boolean {
        val response = client.from("likes")
            .select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }
        return response.decodeList<Like>().isNotEmpty()
    }

    override suspend fun likePost(postId: String, userId: String): Like {
        if (isLiked(postId, userId)) {
            throw IllegalStateException("Post is already liked by this user")
        }

        val likeData = mapOf(
            "post_id" to postId,
            "user_id" to userId
        )

        val inserted = client.from("likes").insert(likeData) {
            select()
        }
        adjustPostLikeCount(postId, 1)
        return inserted.decodeSingle<Like>()
    }

    override suspend fun unlikePost(postId: String, userId: String) {
        client.from("likes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
        adjustPostLikeCount(postId, -1)
    }

    override suspend fun getLikeCount(postId: String): Int {
        val response = client.from("likes")
            .select {
                filter { eq("post_id", postId) }
            }
        return response.decodeList<Like>().size
    }

    private suspend fun adjustPostLikeCount(postId: String, delta: Int) {
        try {
            val result = client.from("posts").select {
                filter { eq("id", postId) }
                single()
            }
            val currentCount = result.decodeAs<Post>().likesCount
            val updated = (currentCount + delta).coerceAtLeast(0)

            client.from("posts").update(mapOf("likes_count" to updated)) {
                filter { eq("id", postId) }
            }
        } catch (e: Exception) {
            Log.e(LIKE_TAG, "Failed to adjust likes count for postId=$postId", e)
        }
    }
}

