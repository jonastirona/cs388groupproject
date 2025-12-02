package com.example.modmycar

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class FriendsListActivity : AppCompatActivity() {

    private val viewModel: FriendsViewModel by viewModels()
    private lateinit var adapter: FriendListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.friendsToolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.friendsRecyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.friendsSwipeRefresh)
        val progressBar = findViewById<View>(R.id.friendsProgress)
        val emptyState = findViewById<TextView>(R.id.friendsEmptyState)
        val viewRequestsButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.viewRequestsButton)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        viewRequestsButton.setOnClickListener {
            startActivity(android.content.Intent(this, FriendRequestsActivity::class.java))
        }

        adapter = FriendListAdapter { profile ->
            viewModel.unfriend(profile.id)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                session?.user?.id?.let { viewModel.setCurrentUserId(it) }
            } catch (_: Exception) {
                Snackbar.make(recyclerView, "Please sign in to view friends.", Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friends.collect { friends ->
                    adapter.submitList(friends)
                    emptyState.isVisible = friends.isEmpty()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    progressBar.isVisible = loading && adapter.itemCount == 0
                    swipeRefresh.isRefreshing = loading && adapter.itemCount > 0
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(recyclerView, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.infoMessage.collect { message ->
                    message?.let {
                        Snackbar.make(recyclerView, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }
    }
}

