package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a car in the user's garage.
 */
@Serializable
data class Car(
    val id: String,
    
    @SerialName("user_id")
    val userId: String,
    
    val make: String,
    val model: String,
    val color: String,
    val year: Int,
    
    @SerialName("image_url")
    val imageUrl: String? = null, // Main car image from storage
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data class for creating a new car (without id and timestamps).
 */
@Serializable
data class CarCreate(
    @SerialName("user_id")
    val userId: String,
    
    val make: String,
    val model: String,
    val color: String,
    val year: Int,
    
    @SerialName("image_url")
    val imageUrl: String? = null
)

/**
 * Data class for updating a car (all fields optional except id).
 */
@Serializable
data class CarUpdate(
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
    val year: Int? = null,
    
    @SerialName("image_url")
    val imageUrl: String? = null
)

