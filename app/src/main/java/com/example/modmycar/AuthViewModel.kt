package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = SupabaseAuthRepository()
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow<Boolean?>(null)
    val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkAuthState()
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            when (val result = authRepository.signUp(email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    _isAuthenticated.value = true
                    // After signup, check if user is actually authenticated
                    checkAuthState()
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                    _isAuthenticated.value = false
                }
            }
            _isLoading.value = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _isAuthenticated.value = true
                    // Get the current user after successful sign in
                    checkAuthState()
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                    _isAuthenticated.value = false
                }
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            when (val result = authRepository.signOut()) {
                is AuthResult.Success -> {
                    _currentUser.value = null
                    _isAuthenticated.value = false
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun checkAuthState() {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentSession()) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    _isAuthenticated.value = result.data != null
                }
                is AuthResult.Error -> {
                    _isAuthenticated.value = false
                    _currentUser.value = null
                }
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }
}

