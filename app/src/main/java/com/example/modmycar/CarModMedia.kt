package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents media (images/videos) linked to a car-mod completion.
 * Media files are stored in Supabase Storage, and URLs are stored here.
 */
@Serializable
data class CarModMedia(
    val id: String,
    
    @SerialName("car_mod_id")
    val carModId: String,
    
    @SerialName("storage_url")
    val storageUrl: String, // Supabase Storage URL
    
    @SerialName("media_type")
    val mediaType: String, // "image" or "video"
    
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null, // For videos, optional thumbnail URL
    
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Data class for creating a new car-mod media entry.
 */
@Serializable
data class CarModMediaCreate(
    @SerialName("car_mod_id")
    val carModId: String,
    
    @SerialName("storage_url")
    val storageUrl: String,
    
    @SerialName("media_type")
    val mediaType: String,
    
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null
)

