package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Follow(
    val id: String? = null,

    @SerialName("follower_id")
    val followerId: String,  // User who is following

    @SerialName("following_id")
    val followingId: String,  // User being followed

    @SerialName("created_at")
    val createdAt: String? = null
)


