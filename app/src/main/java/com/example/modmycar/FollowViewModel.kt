package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowViewModel(
    val followRepository: FollowRepository = SupabaseFollowRepository(SupabaseClient.client)
) : ViewModel() {

    // Map of userId to whether current user is following them
    private val _followingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followingStatus: StateFlow<Map<String, Boolean>> = _followingStatus.asStateFlow()

    // Map of userId to follower count
    private val _followerCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val followerCounts: StateFlow<Map<String, Int>> = _followerCounts.asStateFlow()

    // Map of userId to following count
    private val _followingCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val followingCounts: StateFlow<Map<String, Int>> = _followingCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun checkFollowStatus(followerId: String, followingId: String) {
        viewModelScope.launch {
            try {
                val isFollowing = followRepository.isFollowing(followerId, followingId)
                _followingStatus.value = _followingStatus.value.toMutableMap().apply {
                    put(followingId, isFollowing)
                }
            } catch (e: Exception) {
                android.util.Log.e("FollowViewModel", "Error checking follow status", e)
            }
        }
    }

    fun loadFollowerCount(userId: String) {
        viewModelScope.launch {
            try {
                val count = followRepository.getFollowerCount(userId)
                _followerCounts.value = _followerCounts.value.toMutableMap().apply {
                    put(userId, count)
                }
            } catch (e: Exception) {
                android.util.Log.e("FollowViewModel", "Error loading follower count", e)
            }
        }
    }

    fun loadFollowingCount(userId: String) {
        viewModelScope.launch {
            try {
                val count = followRepository.getFollowingCount(userId)
                _followingCounts.value = _followingCounts.value.toMutableMap().apply {
                    put(userId, count)
                }
            } catch (e: Exception) {
                android.util.Log.e("FollowViewModel", "Error loading following count", e)
            }
        }
    }

    fun toggleFollow(followerId: String, followingId: String) {
        if (followerId == followingId) {
            _error.value = "You cannot follow yourself"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val isCurrentlyFollowing = _followingStatus.value[followingId] ?: false

                if (isCurrentlyFollowing) {
                    // Unfollow
                    followRepository.unfollowUser(followerId, followingId)
                    _followingStatus.value = _followingStatus.value.toMutableMap().apply {
                        put(followingId, false)
                    }
                    // Update follower count
                    val currentCount = _followerCounts.value[followingId] ?: 0
                    _followerCounts.value = _followerCounts.value.toMutableMap().apply {
                        put(followingId, (currentCount - 1).coerceAtLeast(0))
                    }
                } else {
                    // Follow
                    followRepository.followUser(followerId, followingId)
                    _followingStatus.value = _followingStatus.value.toMutableMap().apply {
                        put(followingId, true)
                    }
                    // Update follower count
                    val currentCount = _followerCounts.value[followingId] ?: 0
                    _followerCounts.value = _followerCounts.value.toMutableMap().apply {
                        put(followingId, currentCount + 1)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to ${if (_followingStatus.value[followingId] == true) "unfollow" else "follow"}: ${e.message}"
                android.util.Log.e("FollowViewModel", "Error toggling follow", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isFollowing(userId: String): Boolean {
        return _followingStatus.value[userId] ?: false
    }

    fun getFollowerCount(userId: String): Int {
        return _followerCounts.value[userId] ?: 0
    }

    fun getFollowingCount(userId: String): Int {
        return _followingCounts.value[userId] ?: 0
    }

    fun clearError() {
        _error.value = null
    }
}

