package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a car model in the database.
 * This table is preloaded with available car models.
 */
@Serializable
data class Car(
    val id: String,
    val make: String,
    val model: String
)

