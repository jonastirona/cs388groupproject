package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val repository: FriendRepository = try {
        SupabaseFriendRepository(SupabaseClient.client)
    } catch (e: Exception) {
        LocalFriendRepository()
    }
) : ViewModel() {

    private val _friends = MutableStateFlow<List<UserProfile>>(emptyList())
    val friends: StateFlow<List<UserProfile>> = _friends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        if (userId == currentUserId) return
        currentUserId = userId
        refresh()
    }

    fun refresh() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _friends.value = repository.getFriends(userId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load friends"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun unfriend(friendUserId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.unfriend(userId, friendUserId)
                _friends.value = _friends.value.filterNot { it.id == friendUserId }
                _infoMessage.value = "Friend removed"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to unfriend"
            }
        }
    }

    fun clearMessages() {
        _infoMessage.value = null
        _error.value = null
    }
}




