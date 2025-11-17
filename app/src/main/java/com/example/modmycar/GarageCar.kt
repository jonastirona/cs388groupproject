package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a car owned by a user in their garage.
 */
@Serializable
data class GarageCar(
    val id: String,
    
    @SerialName("user_id")
    val userId: String, // References user id
    
    @SerialName("car_id")
    val carId: String, // References cars table
    
    val color: String? = null,
    val year: Int? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data class for creating a new garage car.
 */
@Serializable
data class GarageCarCreate(
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("car_id")
    val carId: String,
    
    val color: String? = null,
    val year: Int? = null
)

/**
 * Data class for updating a garage car.
 */
@Serializable
data class GarageCarUpdate(
    val color: String? = null,
    val year: Int? = null
)

