package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendSearchAdapter(
    private val onAddFriend: (UserProfile) -> Unit
) : RecyclerView.Adapter<FriendSearchAdapter.ViewHolder>() {

    private val results = mutableListOf<UserProfile>()

    fun submitList(newResults: List<UserProfile>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_search, parent, false)
        return ViewHolder(view, onAddFriend)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size

    class ViewHolder(
        itemView: View,
        private val onAddFriend: (UserProfile) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.searchFriendName)
        private val usernameView: TextView = itemView.findViewById(R.id.searchFriendUsername)
        private val addButton: Button = itemView.findViewById(R.id.addFriendButton)

        fun bind(profile: UserProfile) {
            nameView.text = profile.display_name ?: profile.username ?: "Unknown user"
            usernameView.text = profile.username?.let { "@$it" } ?: ""
            addButton.setOnClickListener { onAddFriend(profile) }
        }
    }
}


