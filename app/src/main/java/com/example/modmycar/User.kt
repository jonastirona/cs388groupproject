package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String? = null,
    val displayName: String? = null
)

@Serializable
data class UserProfile(
    val id: String,
    val username: String? = null,

    @SerialName("display_name")
    val displayName: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    @SerialName("location_updated_at")
    val locationUpdatedAt: String? = null
) {
    val display_name: String?
        get() = displayName
}
