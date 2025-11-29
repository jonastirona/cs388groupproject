package com.example.modmycar

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class FriendSearchActivity : AppCompatActivity() {

    private val viewModel: FriendSearchViewModel by viewModels()
    private lateinit var adapter: FriendSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_search)

        val toolbar = findViewById<MaterialToolbar>(R.id.friendSearchToolbar)
        val searchInput = findViewById<TextInputEditText>(R.id.searchInput)
        val searchButton = findViewById<MaterialButton>(R.id.searchButton)
        val recyclerView = findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        val emptyState = findViewById<TextView>(R.id.searchEmptyState)
        val progressBar = findViewById<ProgressBar>(R.id.searchProgress)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = FriendSearchAdapter { profile ->
            viewModel.addFriend(profile.id)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fun triggerSearch() {
            val query = searchInput.text?.toString().orEmpty()
            viewModel.search(query)
        }

        searchButton.setOnClickListener { triggerSearch() }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                triggerSearch()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                session?.user?.id?.let { viewModel.setCurrentUserId(it) }
            } catch (_: Exception) {
                Snackbar.make(recyclerView, "Please sign in to add friends.", Snackbar.LENGTH_LONG).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { results ->
                    adapter.submitList(results)
                    emptyState.isVisible = results.isEmpty()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSearching.collect { searching ->
                    progressBar.isVisible = searching
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
    }
}



