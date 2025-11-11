package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: UserRepository = SupabaseUserRepository()
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = userRepository.getProfile(userId)) {
                is AuthResult.Success -> {
                    _userProfile.value = result.data
                }
                is AuthResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun createProfile(userId: String, username: String?, displayName: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val profile = UserProfile(
                id = userId,
                username = username,
                display_name = displayName
            )
            when (val result = userRepository.createProfile(profile)) {
                is AuthResult.Success -> {
                    _userProfile.value = result.data
                    _profileSaved.value = true
                }
                is AuthResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(userId: String, username: String?, displayName: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = userRepository.updateProfile(userId, username, displayName)) {
                is AuthResult.Success -> {
                    _userProfile.value = result.data
                    _profileSaved.value = true
                }
                is AuthResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun deleteProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = userRepository.deleteProfile(userId)) {
                is AuthResult.Success -> {
                    _userProfile.value = null
                }
                is AuthResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
    
    fun clearProfileSaved() {
        _profileSaved.value = false
    }
}

