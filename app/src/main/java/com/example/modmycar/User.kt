package com.example.modmycar

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
    val display_name: String? = null
)

