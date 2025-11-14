package com.example.modmycar

data class FriendItem(
    val userId: String,
    val username: String?,
    val displayName: String?,
    val isFollowing: Boolean = false  // Whether current user is following this friend
)


