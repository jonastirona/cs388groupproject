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

class MainActivity : AppCompatActivity(), CommentActionListener, LikeActionListener {

    private val feedViewModel: FeedViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val commentViewModel: CommentViewModel by viewModels()
    private val likeViewModel: LikeViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1) Auth gate
        checkAuthAndNavigate()

        // 2) Recycler setup
        val rv = findViewById<RecyclerView>(R.id.feedRecycler)
        feedAdapter = FeedAdapter(this, this)
        val layoutManager = LinearLayoutManager(this)

        // Get current user ID and update adapter
        lifecycleScope.launch {
            authViewModel.currentUser.collect { user ->
                currentUserId = user?.id
                // Update adapter with new userId if needed
                // Note: For simplicity, we'll recreate the adapter if userId changes
                // In production, you might want to add a method to update userId
            }
        }

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

        // 7) Observe comments updates
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentViewModel.comments.collect { commentsMap ->
                    // Refresh adapter when comments change
                    feedAdapter.notifyDataSetChanged()
                }
            }
        }

        // Observe comment errors
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentViewModel.error.collect { error ->
                    error?.let {
                        Log.e("MainActivity", "Comment error: $it")
                        android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                        commentViewModel.clearError()
                    }
                }
            }
        }

        // 8) Observe likes updates
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                likeViewModel.likedPosts.collect {
                    // Refresh adapter when likes change
                    feedAdapter.notifyDataSetChanged()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                likeViewModel.likeCounts.collect {
                    // Refresh adapter when like counts change
                    feedAdapter.notifyDataSetChanged()
                }
            }
        }

        // Load likes for posts when they're loaded
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.posts.collect { posts ->
                    posts.forEach { post ->
                        // Initialize like count from post data
                        likeViewModel.initializeLikeCount(post.id, post.likesCount)
                    }
                    currentUserId?.let { userId ->
                        posts.forEach { post ->
                            // Check if user has liked each post (only for non-RSS posts)
                            // RSS posts are skipped in checkIfUserLikedPost
                            likeViewModel.checkIfUserLikedPost(post.id, userId)
                        }
                    }
                }
            }
        }

        // Observe like errors
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                likeViewModel.error.collect { error ->
                    error?.let {
                        Log.e("MainActivity", "Like error: $it")
                        android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                        likeViewModel.clearError()
                    }
                }
            }
        }

        // 9) Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 10) Observe auth changes during session
        observeAuthState()
    }

    // CommentActionListener implementation
    override fun onToggleComments(postId: String, isExpanded: Boolean) {
        if (isExpanded) {
            commentViewModel.loadCommentsForPost(postId)
        }
        feedAdapter.toggleComments(postId)
    }

    override fun onPostComment(postId: String, content: String) {
        val userId = currentUserId
        if (userId == null) {
            Log.e("MainActivity", "Cannot post comment: currentUserId is null")
            return
        }
        Log.d("MainActivity", "Posting comment: postId=$postId, userId=$userId, content=$content")
        commentViewModel.addComment(postId, userId, content)
        // Refresh feed to update comment counts
        feedViewModel.refresh()
    }

    override fun getCommentsForPost(postId: String): List<Comment> {
        return commentViewModel.comments.value[postId] ?: emptyList()
    }

    override fun getCurrentUserId(): String? {
        return currentUserId
    }

    // LikeActionListener implementation
    override fun onLikeToggle(postId: String) {
        val userId = currentUserId
        if (userId == null) {
            Log.e("MainActivity", "Cannot toggle like: currentUserId is null")
            return
        }
        Log.d("MainActivity", "Toggling like: postId=$postId, userId=$userId")
        likeViewModel.toggleLike(postId, userId)
        // Refresh feed to update like counts
        feedViewModel.refresh()
    }

    override fun isPostLiked(postId: String): Boolean {
        val userId = currentUserId ?: return false
        return likeViewModel.isPostLikedByUser(postId, userId)
    }

    override fun getLikeCount(postId: String): Int {
        return likeViewModel.getLikeCount(postId)
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