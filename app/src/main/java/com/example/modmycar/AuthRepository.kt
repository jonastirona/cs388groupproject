package com.example.modmycar

import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    suspend fun signUp(email: String, password: String): AuthResult<UserInfo>
    suspend fun signIn(email: String, password: String): AuthResult<Unit>
    suspend fun signOut(): AuthResult<Unit>
    suspend fun getCurrentSession(): AuthResult<UserInfo?>
    fun observeAuthState(): Flow<Boolean>
}

/**
 * Converts technical error messages to user-friendly messages
 */
private fun getErrorMessage(exception: Exception, defaultMessage: String): String {
    val message = exception.message?.lowercase() ?: ""
    val exceptionType = exception.javaClass.simpleName.lowercase()
    
    return when {
        // HTTP 400 errors - usually invalid credentials
        message.contains("400") ||
        message.contains("bad request") ||
        exceptionType.contains("badrequest") ->
            "Invalid email or password. Please check your credentials and try again."
        
        // HTTP 401 errors - unauthorized/invalid credentials
        message.contains("401") ||
        message.contains("unauthorized") ||
        exceptionType.contains("unauthorized") ->
            "Invalid email or password. Please check your credentials and try again."
        
        // HTTP 404 errors - user not found
        message.contains("404") ||
        message.contains("not found") ||
        exceptionType.contains("notfound") ->
            "No account found with this email address. Please sign up first."
        
        // Invalid credentials (common patterns)
        message.contains("invalid login credentials") || 
        message.contains("invalid credentials") ||
        message.contains("email or password") ||
        message.contains("wrong password") ||
        message.contains("invalid password") ||
        message.contains("incorrect password") ||
        message.contains("authentication failed") ->
            "Invalid email or password. Please check your credentials and try again."
        
        // User not found
        message.contains("user not found") ||
        message.contains("no user found") ||
        message.contains("email not found") ||
        message.contains("user does not exist") ->
            "No account found with this email address. Please sign up to create an account."
        
        // Email format errors
        message.contains("invalid email") ||
        message.contains("email format") ||
        message.contains("malformed email") ||
        message.contains("invalid email format") ->
            "Please enter a valid email address."
        
        // Network errors
        message.contains("network") ||
        message.contains("connection") ||
        message.contains("timeout") ||
        message.contains("unable to resolve host") ||
        message.contains("failed to connect") ||
        message.contains("no internet") ||
        message.contains("networkerror") ||
        exceptionType.contains("timeout") ||
        exceptionType.contains("connect") ->
            "Network error. Please check your internet connection and try again."
        
        // Account already exists (for sign up)
        message.contains("user already registered") ||
        message.contains("already registered") ||
        message.contains("email already exists") ||
        message.contains("already exists") ||
        message.contains("user already exists") ||
        message.contains("duplicate") ->
            "An account with this email already exists. Please sign in to continue."
        
        // Password too weak
        (message.contains("password") && message.contains("weak")) ||
        (message.contains("password") && message.contains("short")) ||
        (message.contains("password") && message.contains("minimum")) ||
        message.contains("password too short") ||
        message.contains("password requirements") ->
            "Password is too weak. Please use a stronger password (at least 6 characters)."
        
        // HTTP 429 - Rate limiting
        message.contains("429") ||
        message.contains("too many requests") ||
        message.contains("rate limit") ->
            "Too many sign-in attempts. Please wait a moment and try again."
        
        // HTTP 500+ - Server errors
        message.contains("500") ||
        message.contains("502") ||
        message.contains("503") ||
        message.contains("server error") ||
        message.contains("internal server error") ->
            "Server error. Please try again later."
        
        // Generic Supabase errors
        message.contains("supabase") && message.contains("error") ->
            "Authentication service error. Please try again later."
        
        // Default: use the exception message if it's reasonable, otherwise use default
        else -> defaultMessage
    }
}

class SupabaseAuthRepository(
    private val supabaseClient: SupabaseClientType
) : AuthRepository {
    
    constructor() : this(SupabaseClient.client)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()

    override suspend fun signUp(email: String, password: String): AuthResult<UserInfo> {
        return try {
            val user = supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // No redirectTo - uses Supabase's default hosted verification page
                // Note: If email verification is disabled in Supabase dashboard,
                // the user will be automatically authenticated
            } ?: throw Exception("User not returned from sign up")
            
            // After signup, check if user is authenticated
            // If email verification is disabled, user will be authenticated immediately
            // If email verification is enabled, user needs to verify email first
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                // User is authenticated (email verification disabled or already verified)
                checkAuthState()
            } else {
                // Email verification required - user needs to verify email first
                // Still return success but user won't be fully authenticated until email is verified
                checkAuthState()
            }
            
            AuthResult.Success(user)
        } catch (e: Exception) {
            val userMessage = getErrorMessage(e, "Unable to create account. Please try again.")
            AuthResult.Error(userMessage, e)
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            checkAuthState()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            val userMessage = getErrorMessage(e, "Unable to sign in. Please check your credentials and try again.")
            AuthResult.Error(userMessage, e)
        }
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signOut()
            checkAuthState()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            val userMessage = getErrorMessage(e, "Unable to sign out. Please try again.")
            AuthResult.Error(userMessage, e)
        }
    }

    override suspend fun getCurrentSession(): AuthResult<UserInfo?> {
        return try {
            val session = supabaseClient.auth.currentSessionOrNull()
            val user = session?.user
            checkAuthState()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get session: ${e.message}", e)
        }
    }

    override fun observeAuthState(): Flow<Boolean> {
        return isAuthenticated
    }

    private suspend fun checkAuthState() {
        val isAuth = try {
            supabaseClient.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }
        _isAuthenticated.value = isAuth
    }
}

