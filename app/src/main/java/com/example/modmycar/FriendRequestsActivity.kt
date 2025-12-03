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

class FriendRequestsActivity : AppCompatActivity() {

    private val viewModel: FriendRequestsViewModel by viewModels()
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        val toolbar = findViewById<MaterialToolbar>(R.id.friendRequestsToolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.friendRequestsRecyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.friendRequestsSwipeRefresh)
        val progressBar = findViewById<View>(R.id.friendRequestsProgress)
        val emptyState = findViewById<TextView>(R.id.friendRequestsEmptyState)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = FriendRequestsAdapter(
            onAccept = { requestId, profile ->
                viewModel.acceptRequest(requestId, profile)
            },
            onReject = { requestId ->
                viewModel.rejectRequest(requestId)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.loadPendingRequests() }

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                session?.user?.id?.let { viewModel.setCurrentUserId(it) }
            } catch (_: Exception) {
                Snackbar.make(recyclerView, "Please sign in to view friend requests.", Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingRequests.collect { requests ->
                    adapter.submitList(requests)
                    emptyState.isVisible = requests.isEmpty()
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

