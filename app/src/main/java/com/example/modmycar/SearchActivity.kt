package com.example.modmycar

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle

class SearchActivity : AppCompatActivity(), FriendActionListener {

    private val authViewModel: AuthViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val userRepository: UserRepository = SupabaseUserRepository()
    private val followRepository: FollowRepository = SupabaseFollowRepository(SupabaseClient.client)

    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchButton: Button
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var noResultsText: TextView
    private lateinit var progressBar: View
    private lateinit var searchAdapter: SearchAdapter

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        noResultsText = findViewById(R.id.noResultsText)
        progressBar = findViewById(R.id.progressBar)

        searchAdapter = SearchAdapter(this)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchAdapter

        searchButton.setOnClickListener {
            performSearch()
        }

        // Search on Enter key press
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        observeViewModels()
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    currentUserId = user?.id
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                followViewModel.followingStatus.collect {
                    // Reload search results when follow status changes
                    val query = searchEditText.text?.toString()?.trim()
                    if (!query.isNullOrBlank()) {
                        performSearch()
                    }
                }
            }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text?.toString()?.trim()
        if (query.isNullOrBlank()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            noResultsText.visibility = View.GONE

            try {
                // Search for users
                val profiles = userRepository.searchUsers(query)
                
                if (profiles.isEmpty()) {
                    noResultsText.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                    searchAdapter.setSearchResults(emptyList())
                } else {
                    noResultsText.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    
                    // Convert to SearchResultItem and check follow status
                    val currentId = currentUserId
                    val searchResults = profiles.map { profile ->
                        val isFollowing = if (currentId != null) {
                            followRepository.isFollowing(currentId, profile.id)
                        } else {
                            false
                        }
                        SearchResultItem(
                            userId = profile.id,
                            username = profile.username,
                            displayName = profile.display_name,
                            isFollowing = isFollowing
                        )
                    }
                    searchAdapter.setSearchResults(searchResults)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SearchActivity, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("SearchActivity", "Error searching users", e)
                emptyText.visibility = View.VISIBLE
                noResultsText.visibility = View.GONE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onFollowToggle(userId: String, isCurrentlyFollowing: Boolean) {
        val currentId = currentUserId ?: return
        followViewModel.toggleFollow(currentId, userId)
        // Search results will reload automatically via the observer
    }

    override fun getCurrentUserId(): String? {
        return currentUserId
    }
}


