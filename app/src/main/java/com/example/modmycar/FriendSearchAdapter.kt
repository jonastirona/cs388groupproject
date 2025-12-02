package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendSearchAdapter(
    private val onSendRequest: (UserProfile) -> Unit,
    private val onCancelRequest: (UserProfile) -> Unit,
    private val onAcceptRequest: (String, UserProfile) -> Unit = { _, _ -> } // requestId, profile
) : RecyclerView.Adapter<FriendSearchAdapter.ViewHolder>() {

    private val results = mutableListOf<UserWithFriendshipStatus>()

    fun submitList(newResults: List<UserWithFriendshipStatus>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_search, parent, false)
        return ViewHolder(view, onSendRequest, onCancelRequest, onAcceptRequest)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    class ViewHolder(
        itemView: View,
        private val onSendRequest: (UserProfile) -> Unit,
        private val onCancelRequest: (UserProfile) -> Unit,
        private val onAcceptRequest: (String, UserProfile) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.searchFriendName)
        private val usernameView: TextView = itemView.findViewById(R.id.searchFriendUsername)
        private val addButton: Button = itemView.findViewById(R.id.addFriendButton)

        fun bind(result: UserWithFriendshipStatus) {
            val profile = result.user
            nameView.text = profile.display_name ?: profile.username ?: "Unknown user"
            usernameView.text = profile.username?.let { "@$it" } ?: ""
            
            // Update button based on friendship status
            when (result.friendshipStatus) {
                FriendshipStatus.NONE -> {
                    addButton.text = "Send Request"
                    addButton.isEnabled = true
                    addButton.setOnClickListener { onSendRequest(profile) }
                }
                FriendshipStatus.FRIENDS -> {
                    addButton.text = "Friends"
                    addButton.isEnabled = false
                    addButton.setOnClickListener(null)
                }
                FriendshipStatus.PENDING_OUTGOING -> {
                    addButton.text = "Pending"
                    addButton.isEnabled = true
                    addButton.setOnClickListener { onCancelRequest(profile) }
                }
                FriendshipStatus.PENDING_INCOMING -> {
                    addButton.text = "Accept"
                    addButton.isEnabled = true
                    val requestId = result.requestId ?: ""
                    addButton.setOnClickListener { 
                        if (requestId.isNotEmpty()) {
                            onAcceptRequest(requestId, profile)
                        }
                    }
                }
            }
        }
    }
}




