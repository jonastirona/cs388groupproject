package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

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
            }
        return response.decodeList<Like>().isNotEmpty()
    }

    override suspend fun likePost(postId: String, userId: String): Like {
        // Check if already liked
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
        return inserted.decodeSingle<Like>()
    }

    override suspend fun unlikePost(postId: String, userId: String) {
        client.from("likes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun getLikeCount(postId: String): Int {
        val response = client.from("likes")
            .select {
                filter { eq("post_id", postId) }
            }
        return response.decodeList<Like>().size
    }
}

