package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

interface FollowRepository {
    suspend fun followUser(followerId: String, followingId: String): Follow
    suspend fun unfollowUser(followerId: String, followingId: String)
    suspend fun isFollowing(followerId: String, followingId: String): Boolean
    suspend fun getFollowers(userId: String): List<Follow>
    suspend fun getFollowing(userId: String): List<Follow>
    suspend fun getFollowerCount(userId: String): Int
    suspend fun getFollowingCount(userId: String): Int
}

class SupabaseFollowRepository(
    private val client: SupabaseClient
) : FollowRepository {

    override suspend fun followUser(followerId: String, followingId: String): Follow {
        val newFollow = Follow(
            followerId = followerId,
            followingId = followingId
        )
        val inserted = client.from("follows").insert(newFollow) {
            select()
        }
        return inserted.decodeSingle<Follow>()
    }

    override suspend fun unfollowUser(followerId: String, followingId: String) {
        client.from("follows").delete {
            filter {
                eq("follower_id", followerId)
                eq("following_id", followingId)
            }
        }
    }

    override suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        val follow = client.from("follows")
            .select {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
                single()
            }
            .decodeSingleOrNull<Follow>()
        return follow != null
    }

    override suspend fun getFollowers(userId: String): List<Follow> {
        return client.from("follows")
            .select {
                filter {
                    eq("following_id", userId)
                }
            }
            .decodeList<Follow>()
    }

    override suspend fun getFollowing(userId: String): List<Follow> {
        return client.from("follows")
            .select {
                filter {
                    eq("follower_id", userId)
                }
            }
            .decodeList<Follow>()
    }

    override suspend fun getFollowerCount(userId: String): Int {
        return getFollowers(userId).size
    }

    override suspend fun getFollowingCount(userId: String): Int {
        return getFollowing(userId).size
    }
}


