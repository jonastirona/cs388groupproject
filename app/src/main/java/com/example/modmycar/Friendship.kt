package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Friendship(
    val id: String,

    @SerialName("requester_id")
    val requesterId: String,

    @SerialName("addressee_id")
    val addresseeId: String,

    val status: String = "accepted",

    @SerialName("created_at")
    val createdAt: String
)


