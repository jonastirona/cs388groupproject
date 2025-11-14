package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

interface LikeRepository {
    suspend fun getLikesForPost(postId: String): List<Like>
    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean
    suspend fun addLike(postId: String, userId: String): Like
    suspend fun removeLike(postId: String, userId: String)
    suspend fun getLikeCount(postId: String): Int
}

class SupabaseLikeRepository(
    private val client: SupabaseClient
) : LikeRepository {

    override suspend fun getLikesForPost(postId: String): List<Like> {
        return client.from("likes")
            .select {
                filter {
                    eq("post_id", postId)
                }
            }
            .decodeList<Like>()
    }

    override suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        val like = client.from("likes")
            .select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
                single()
            }
            .decodeSingleOrNull<Like>()
        return like != null
    }

    override suspend fun addLike(postId: String, userId: String): Like {
        val newLike = Like(
            postId = postId,
            userId = userId
        )
        val inserted = client.from("likes").insert(newLike) {
            select()
        }
        return inserted.decodeSingle<Like>()
    }

    override suspend fun removeLike(postId: String, userId: String) {
        client.from("likes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun getLikeCount(postId: String): Int {
        // Get all likes for the post and return the count
        // This is efficient enough for most use cases
        return getLikesForPost(postId).size
    }
}

