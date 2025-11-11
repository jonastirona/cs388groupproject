package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.Lifecycle

class MainActivity : AppCompatActivity() {

    private val feedViewModel: FeedViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) Auth gate
        checkAuthAndNavigate()

        // 2) Recycler setup
        val rv = findViewById<RecyclerView>(R.id.feedRecycler)
        feedAdapter = FeedAdapter()
        val layoutManager = LinearLayoutManager(this)

        rv.layoutManager = layoutManager
        rv.adapter = feedAdapter

        // 3) Profile FAB
        findViewById<FloatingActionButton>(R.id.profileFab).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // 4) Infinite scroll
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return // only when scrolling down

                val total = feedAdapter.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val threshold = 5
                if (lastVisible >= total - threshold) {
                    feedViewModel.loadNextPage()
                }
            }
        })

        // 5) Initial load
        feedViewModel.refresh()

        // 6) Collect posts and render
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.posts.collect { posts ->
                    feedAdapter.setItems(posts)
                    Log.d("FeedVM", "Rendered ${posts.size} posts")
                }
            }
        }

        // 7) Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 8) Observe auth changes during session
        observeAuthState()
    }

    private fun checkAuthAndNavigate() {
        lifecycleScope.launch {
            authViewModel.checkAuthState()
            authViewModel.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated == false) navigateToLogin()
            }
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isAuthenticated.collect { isAuthenticated ->
                    if (isAuthenticated == false) navigateToLogin()
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}