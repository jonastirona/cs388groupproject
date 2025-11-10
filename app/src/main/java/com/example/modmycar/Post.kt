package com.example.modmycar

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val userId: String,
    val carId: String? = null,
    val caption: String? = null,
    val media: List<MediaItem>,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val visibility: String = "public",
    val status: String = "active",
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class MediaItem(
    val type: String, // "image" or "audio"
    val url: String,
    val durationSec: Double? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Int? = null
)
