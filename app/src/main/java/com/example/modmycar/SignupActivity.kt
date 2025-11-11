package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var errorText: android.widget.TextView
    private lateinit var progressBar: android.widget.ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        signupButton = findViewById(R.id.signupButton)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)

        findViewById<android.widget.TextView>(R.id.loginPromptText).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text?.toString() ?: ""
            val password = passwordEditText.text?.toString() ?: ""
            val confirmPassword = confirmPasswordEditText.text?.toString() ?: ""

            if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showError("Passwords do not match")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            authViewModel.signUp(email, password)
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                authViewModel.isAuthenticated.collect { isAuthenticated ->
                    if (isAuthenticated == true) {
                        // Check profile and navigate accordingly
                        checkProfileAndNavigate()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                authViewModel.authError.collect { error ->
                    error?.let {
                        showError(it)
                        authViewModel.clearError()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    signupButton.isEnabled = !isLoading
                }
            }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private var hasNavigated = false
    
    private fun checkProfileAndNavigate() {
        if (hasNavigated) return
        
        lifecycleScope.launch {
            val currentUser = authViewModel.currentUser.value
            if (currentUser != null) {
                // Load profile to check if it's customized
                userViewModel.loadProfile(currentUser.id)
                
                // Wait for profile to load (or confirm it doesn't exist)
                var profile: UserProfile? = null
                var attempts = 0
                while (attempts < 10 && !hasNavigated) {
                    kotlinx.coroutines.delay(200)
                    profile = userViewModel.userProfile.value
                    if (profile != null || userViewModel.isLoading.value == false) {
                        break
                    }
                    attempts++
                }
                
                if (!hasNavigated) {
                    val isProfileCustomized = profile != null && 
                        (profile.username != null || profile.display_name != null)
                    
                    hasNavigated = true
                    if (isProfileCustomized) {
                        navigateToMain()
                    } else {
                        navigateToProfile()
                    }
                }
            } else {
                // Wait a bit for currentUser to be set
                kotlinx.coroutines.delay(300)
                val user = authViewModel.currentUser.value
                if (user != null && !hasNavigated) {
                    checkProfileAndNavigate()
                } else if (!hasNavigated) {
                    hasNavigated = true
                    navigateToMain()
                }
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

