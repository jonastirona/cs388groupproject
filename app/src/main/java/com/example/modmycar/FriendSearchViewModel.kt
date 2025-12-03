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

    private val _searchResults = MutableStateFlow<List<UserWithFriendshipStatus>>(emptyList())
    val searchResults: StateFlow<List<UserWithFriendshipStatus>> = _searchResults.asStateFlow()

    fun search(query: String) {
        val userId = currentUserId ?: return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            _results.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            try {
                val exclude = currentFriendIds + userId
                val resultsWithStatus = repository.searchUsers(trimmed, exclude.toSet(), userId)
                _searchResults.value = resultsWithStatus
                _results.value = resultsWithStatus.map { it.user }
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                repository.sendFriendRequest(userId, targetUserId)
                // Update the status in search results
                _searchResults.value = _searchResults.value.map { result ->
                    if (result.user.id == targetUserId) {
                        result.copy(friendshipStatus = FriendshipStatus.PENDING_OUTGOING)
                    } else {
                        result
                    }
                }
                _infoMessage.value = "Friend request sent!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to send friend request"
            }
        }
    }

    fun cancelFriendRequest(targetUserId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                val sentRequests = repository.getSentRequests(userId)
                val requestPair = sentRequests.find { it.first.id == targetUserId }
                
                if (requestPair != null) {
                    repository.cancelFriendRequest(requestPair.second)
                    // Update the status in search results
                    _searchResults.value = _searchResults.value.map { result ->
                        if (result.user.id == targetUserId) {
                            result.copy(friendshipStatus = FriendshipStatus.NONE)
                        } else {
                            result
                        }
                    }
                    _infoMessage.value = "Friend request cancelled"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to cancel friend request"
            }
        }
    }

    fun acceptFriendRequest(requestId: String, targetUserId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(requestId, userId)
                // Update the status in search results
                _searchResults.value = _searchResults.value.map { result ->
                    if (result.user.id == targetUserId) {
                        result.copy(friendshipStatus = FriendshipStatus.FRIENDS)
                    } else {
                        result
                    }
                }
                currentFriendIds += targetUserId
                _infoMessage.value = "Friend request accepted!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to accept friend request"
            }
        }
    }

    fun clearMessages() {
        _infoMessage.value = null
        _error.value = null
    }
}




