package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendSearchViewModel(
    private val repository: FriendRepository = try {
        SupabaseFriendRepository(SupabaseClient.client)
    } catch (e: Exception) {
        LocalFriendRepository()
    }
) : ViewModel() {

    private val _results = MutableStateFlow<List<UserProfile>>(emptyList())
    val results: StateFlow<List<UserProfile>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String? = null
    private val currentFriendIds = mutableSetOf<String>()

    fun setCurrentUserId(userId: String) {
        if (userId == currentUserId) return
        currentUserId = userId
        preloadFriends()
    }

    private fun preloadFriends() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val friends = repository.getFriends(userId)
                currentFriendIds.clear()
                currentFriendIds.addAll(friends.map { it.id })
            } catch (_: Exception) {
                // Ignore preload errors; search will still work
            }
        }
    }

    fun search(query: String) {
        val userId = currentUserId ?: return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _results.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            try {
                val exclude = currentFriendIds + userId
                _results.value = repository.searchUsers(trimmed, exclude.toSet())
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addFriend(targetUserId: String) {
        val userId = currentUserId ?: return
        if (currentFriendIds.contains(targetUserId)) return

        viewModelScope.launch {
            try {
                repository.addFriend(userId, targetUserId)
                currentFriendIds += targetUserId
                _results.value = _results.value.filterNot { it.id == targetUserId }
                _infoMessage.value = "Friend added!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to add friend"
            }
        }
    }

    fun clearMessages() {
        _infoMessage.value = null
        _error.value = null
    }
}



