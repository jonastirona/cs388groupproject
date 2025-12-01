package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("car_id")
    val carId: String? = null,

    val caption: String? = null,

    val media: List<MediaItem> = emptyList(),

    @SerialName("likes_count")
    val likesCount: Int = 0,

    @SerialName("comments_count")
    val commentsCount: Int = 0,

    val visibility: String = "public",
    val status: String = "active",

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("profiles")
    val authorProfile: UserProfile? = null
)

@Serializable
data class MediaItem(
    val type: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,

    @SerialName("duration_sec")
    val durationSec: Double? = null,

    @SerialName("size_bytes")
    val sizeBytes: Int? = null
)
