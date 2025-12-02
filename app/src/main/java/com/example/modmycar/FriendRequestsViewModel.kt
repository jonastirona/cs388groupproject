package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendRequestsViewModel(
    private val repository: FriendRepository = try {
        SupabaseFriendRepository(SupabaseClient.client)
    } catch (e: Exception) {
        LocalFriendRepository()
    }
) : ViewModel() {

    private val _pendingRequests = MutableStateFlow<List<Pair<UserProfile, String>>>(emptyList())
    val pendingRequests: StateFlow<List<Pair<UserProfile, String>>> = _pendingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
        loadPendingRequests()
    }

    fun loadPendingRequests() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _pendingRequests.value = repository.getPendingRequests(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load friend requests"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptRequest(requestId: String, userProfile: UserProfile) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(requestId, userId)
                _pendingRequests.value = _pendingRequests.value.filterNot { it.second == requestId }
                _infoMessage.value = "Friend request accepted!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to accept friend request"
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            try {
                repository.rejectFriendRequest(requestId)
                _pendingRequests.value = _pendingRequests.value.filterNot { it.second == requestId }
                _infoMessage.value = "Friend request rejected"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to reject friend request"
            }
        }
    }

    fun clearMessages() {
        _infoMessage.value = null
        _error.value = null
    }
}

