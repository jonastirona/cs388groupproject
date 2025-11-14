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

class ProfileActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()

    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var displayNameEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var searchButton: MaterialButton
    private lateinit var friendsButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var homeButton: MaterialButton
    private lateinit var errorText: android.widget.TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var followersCountText: android.widget.TextView
    private lateinit var followingCountText: android.widget.TextView

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        emailEditText = findViewById(R.id.emailEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        displayNameEditText = findViewById(R.id.displayNameEditText)
        saveButton = findViewById(R.id.saveButton)
        searchButton = findViewById(R.id.searchButton)
        friendsButton = findViewById(R.id.friendsButton)
        logoutButton = findViewById(R.id.logoutButton)
        homeButton = findViewById(R.id.homeButton)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)
        followersCountText = findViewById(R.id.followersCountText)
        followingCountText = findViewById(R.id.followingCountText)

        saveButton.setOnClickListener {
            currentUserId?.let { userId ->
                val username = usernameEditText.text?.toString()?.takeIf { it.isNotBlank() }
                val displayName = displayNameEditText.text?.toString()?.takeIf { it.isNotBlank() }

                // If profile doesn't exist, create it; otherwise update it
                val currentProfile = userViewModel.userProfile.value
                if (currentProfile == null) {
                    userViewModel.createProfile(userId, username, displayName)
                } else {
                    userViewModel.updateProfile(userId, username, displayName)
                }
            }
        }

        logoutButton.setOnClickListener {
            authViewModel.signOut()
        }

        searchButton.setOnClickListener {
            navigateToSearch()
        }

        friendsButton.setOnClickListener {
            navigateToFriendsList()
        }

        homeButton.setOnClickListener {
            navigateToHome()
        }

        observeViewModels()
    }

    private fun observeViewModels() {
        // Observe auth state to get current user
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    user?.let {
                        currentUserId = it.id
                        emailEditText.setText(it.email)
                        userViewModel.loadProfile(it.id)
                        // Load follower/following counts
                        followViewModel.loadFollowerCount(it.id)
                        followViewModel.loadFollowingCount(it.id)
                    } ?: run {
                        // Not authenticated, go to login
                        navigateToLogin()
                    }
                }
            }
        }

        // Observe user profile
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                userViewModel.userProfile.collect { profile ->
                    profile?.let {
                        usernameEditText.setText(it.username ?: "")
                        displayNameEditText.setText(it.display_name ?: "")
                    } ?: run {
                        // Profile doesn't exist, create it when user saves
                        currentUserId?.let { userId ->
                            val email = authViewModel.currentUser.value?.email ?: ""
                            if (email.isNotBlank()) {
                                // Profile will be created on first save
                            }
                        }
                    }
                }
            }
        }

        // Observe errors
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                userViewModel.error.collect { error ->
                    error?.let {
                        showError(it)
                        userViewModel.clearError()
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
                authViewModel.isAuthenticated.collect { isAuthenticated ->
                    if (isAuthenticated == false) {
                        navigateToLogin()
                    }
                }
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                userViewModel.isLoading.collect { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    saveButton.isEnabled = !isLoading
                }
            }
        }

        // Observe profile saved state to show toast
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                userViewModel.profileSaved.collect { saved ->
                    if (saved) {
                        showProfileSavedToast()
                        userViewModel.clearProfileSaved()
                    }
                }
            }
        }

        // Observe follower counts
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                followViewModel.followerCounts.collect { counts ->
                    currentUserId?.let { userId ->
                        counts[userId]?.let { count ->
                            followersCountText.text = count.toString()
                        }
                    }
                }
            }
        }

        // Observe following counts
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                followViewModel.followingCounts.collect { counts ->
                    currentUserId?.let { userId ->
                        counts[userId]?.let { count ->
                            followingCountText.text = count.toString()
                        }
                    }
                }
            }
        }

        // Observe follow errors
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                followViewModel.error.collect { error ->
                    error?.let {
                        showError(it)
                        followViewModel.clearError()
                    }
                }
            }
        }

        // Check auth state on start
        authViewModel.checkAuthState()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun showProfileSavedToast() {
        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToSearch() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToFriendsList() {
        val intent = Intent(this, FriendsListActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

