package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MyPostsActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.myPostsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Observe auth state to get current user
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    user?.let {
                        // Load the fragment with user ID
                        if (supportFragmentManager.findFragmentById(R.id.myPostsFragmentContainer) == null) {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.myPostsFragmentContainer, PostsFragment.newInstance(it.id))
                                .commit()
                        }
                    } ?: run {
                        // Not authenticated, go to login
                        navigateToLogin()
                    }
                }
            }
        }

        // Check auth state on start
        authViewModel.checkAuthState()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

