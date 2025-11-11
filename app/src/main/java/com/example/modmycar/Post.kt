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

    // Supabase stores this as JSONB; our serializer will map it into this list
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
    val updatedAt: String? = null
)

@Serializable
data class MediaItem(
    // Matches what we inserted: {"type":"image","url":"...","width":800,"height":600}
    val type: String,          // "image" | "audio"
    val url: String,
    val width: Int? = null,
    val height: Int? = null,

    // Optional fields you might use later for audio/images
    @SerialName("duration_sec")
    val durationSec: Double? = null,

    @SerialName("size_bytes")
    val sizeBytes: Int? = null
)