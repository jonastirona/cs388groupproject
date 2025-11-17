package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a mod completed by a user for their garage car.
 */
@Serializable
data class GarageMod(
    val id: String,
    
    @SerialName("user_id")
    val userId: String, // References user id
    
    @SerialName("mod_id")
    val modId: String, // References mods table
    
    @SerialName("completed_at")
    val completedAt: String? = null, // Timestamp when mod was completed
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data class for creating a new garage mod.
 */
@Serializable
data class GarageModCreate(
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("mod_id")
    val modId: String,
    
    @SerialName("completed_at")
    val completedAt: String? = null
)

/**
 * Data class for updating a garage mod.
 */
@Serializable
data class GarageModUpdate(
    @SerialName("completed_at")
    val completedAt: String? = null
)

