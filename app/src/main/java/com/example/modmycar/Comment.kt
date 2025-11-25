package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",

    @SerialName("post_id")
    val postId: String,

    @SerialName("user_id")
    val userId: String,

    val content: String,

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("profiles")
    val authorProfile: CommentAuthor? = null
)

@Serializable
data class CommentAuthor(
    val id: String? = null,
    val username: String? = null,
    @SerialName("display_name")
    val displayName: String? = null
)

