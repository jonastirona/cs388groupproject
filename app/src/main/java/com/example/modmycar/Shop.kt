package com.example.modmycar

import kotlinx.serialization.Serializable

/**
 * Data class representing a shop (auto-tuning, parts store, etc.)
 * with location and contact information.
 */
@Serializable
data class Shop(
    val id: String,
    val name: String,
    val address: String,
    val phone: String? = null,
    val website: String? = null,
    val rating: Double? = null,
    val latitude: Double,
    val longitude: Double,
    val placeId: String,
    val openingHours: List<String>? = null,
    val types: List<String>? = null
)

