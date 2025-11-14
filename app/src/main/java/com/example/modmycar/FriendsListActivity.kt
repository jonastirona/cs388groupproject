package com.example.modmycar

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle

class FriendsListActivity : AppCompatActivity(), FriendActionListener {

    private val authViewModel: AuthViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val userRepository: UserRepository = SupabaseUserRepository()
    private val followRepository: FollowRepository = SupabaseFollowRepository(SupabaseClient.client)

    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var btnFollowing: Button
    private lateinit var btnFollowers: Button
    private lateinit var emptyText: TextView
    private lateinit var progressBar: View
    private lateinit var friendsAdapter: FriendsAdapter

    private var currentUserId: String? = null
    private var currentTab: Tab = Tab.FOLLOWING

    enum class Tab {
        FOLLOWING, FOLLOWERS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends_list)

        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        btnFollowing = findViewById(R.id.btnFollowing)
        btnFollowers = findViewById(R.id.btnFollowers)
        emptyText = findViewById(R.id.emptyText)
        progressBar = findViewById(R.id.progressBar)

        friendsAdapter = FriendsAdapter(this)
        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.adapter = friendsAdapter

        btnFollowing.setOnClickListener {
            switchTab(Tab.FOLLOWING)
        }

        btnFollowers.setOnClickListener {
            switchTab(Tab.FOLLOWERS)
        }

        // Set initial tab state
        updateTabButtons()

        observeViewModels()
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    currentUserId = user?.id
                    if (user != null) {
                        loadFriends()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                followViewModel.followingStatus.collect {
                    // Reload friends when follow status changes
                    loadFriends()
                }
            }
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        updateTabButtons()
        loadFriends()
    }

    private fun updateTabButtons() {
        btnFollowing.isSelected = currentTab == Tab.FOLLOWING
        btnFollowers.isSelected = currentTab == Tab.FOLLOWERS
    }

    private fun loadFriends() {
        val userId = currentUserId ?: return

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            emptyText.visibility = View.GONE

            try {
                val friendItems = mutableListOf<FriendItem>()

                if (currentTab == Tab.FOLLOWING) {
                    // Get users that current user is following
                    val following = followRepository.getFollowing(userId)
                    for (follow in following) {
                        val profileResult = userRepository.getProfile(follow.followingId)
                        if (profileResult is AuthResult.Success) {
                            val profile = profileResult.data
                            friendItems.add(
                                FriendItem(
                                    userId = follow.followingId,
                                    username = profile?.username,
                                    displayName = profile?.display_name,
                                    isFollowing = true
                                )
                            )
                        }
                    }
                } else {
                    // Get users that are following current user
                    val followers = followRepository.getFollowers(userId)
                    for (follow in followers) {
                        val profileResult = userRepository.getProfile(follow.followerId)
                        if (profileResult is AuthResult.Success) {
                            val profile = profileResult.data
                            // Check if current user is also following them back
                            val isFollowingBack = followRepository.isFollowing(userId, follow.followerId)
                            friendItems.add(
                                FriendItem(
                                    userId = follow.followerId,
                                    username = profile?.username,
                                    displayName = profile?.display_name,
                                    isFollowing = isFollowingBack
                                )
                            )
                        }
                    }
                }

                friendsAdapter.setFriends(friendItems)
                emptyText.visibility = if (friendItems.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@FriendsListActivity, "Failed to load friends: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("FriendsListActivity", "Error loading friends", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onFollowToggle(userId: String, isCurrentlyFollowing: Boolean) {
        val currentId = currentUserId ?: return
        followViewModel.toggleFollow(currentId, userId)
        // Friends list will reload automatically via the observer
    }

    override fun getCurrentUserId(): String? {
        return currentUserId
    }
}

