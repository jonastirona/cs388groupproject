package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendRequestsAdapter(
    private val onAccept: (String, UserProfile) -> Unit, // requestId, profile
    private val onReject: (String) -> Unit // requestId
) : RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder>() {

    private val requests = mutableListOf<Pair<UserProfile, String>>() // profile, requestId

    fun submitList(newRequests: List<Pair<UserProfile, String>>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view, onAccept, onReject)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (profile, requestId) = requests[position]
        holder.bind(profile, requestId)
    }

    override fun getItemCount(): Int = requests.size

    class ViewHolder(
        itemView: View,
        private val onAccept: (String, UserProfile) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.requestFriendName)
        private val usernameView: TextView = itemView.findViewById(R.id.requestFriendUsername)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptRequestButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectRequestButton)

        fun bind(profile: UserProfile, requestId: String) {
            nameView.text = profile.display_name ?: profile.username ?: "Unknown user"
            usernameView.text = profile.username?.let { "@$it" } ?: ""
            
            acceptButton.setOnClickListener { onAccept(requestId, profile) }
            rejectButton.setOnClickListener { onReject(requestId) }
        }
    }
}

