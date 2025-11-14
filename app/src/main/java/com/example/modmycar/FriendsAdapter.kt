package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface FriendActionListener {
    fun onFollowToggle(userId: String, isCurrentlyFollowing: Boolean)
    fun getCurrentUserId(): String?
}

class FriendsAdapter(
    private val friendActionListener: FriendActionListener? = null
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val friends = mutableListOf<FriendItem>()

    fun setFriends(newFriends: List<FriendItem>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        val currentUserId = friendActionListener?.getCurrentUserId()
        
        // Don't show follow button for current user
        val isCurrentUser = currentUserId == friend.userId
        
        holder.bind(friend, friendActionListener, isCurrentUser)
    }

    override fun getItemCount() = friends.size

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val usernameText: TextView = view.findViewById(R.id.tvFriendUsername)
        private val displayNameText: TextView = view.findViewById(R.id.tvFriendDisplayName)
        private val followButton: Button = view.findViewById(R.id.btnFollowFriend)

        fun bind(
            friend: FriendItem,
            listener: FriendActionListener?,
            isCurrentUser: Boolean
        ) {
            usernameText.text = friend.username ?: friend.userId.take(8)
            displayNameText.text = friend.displayName ?: ""

            if (isCurrentUser) {
                followButton.visibility = View.GONE
            } else {
                followButton.visibility = View.VISIBLE
                followButton.text = if (friend.isFollowing) "Unfollow" else "Follow"
                followButton.setOnClickListener {
                    listener?.onFollowToggle(friend.userId, friend.isFollowing)
                }
            }
        }
    }
}


