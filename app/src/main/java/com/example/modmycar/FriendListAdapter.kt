package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {

    private val friends = mutableListOf<UserProfile>()

    fun submitList(newFriends: List<UserProfile>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.friendName)
        private val usernameView: TextView = itemView.findViewById(R.id.friendUsername)

        fun bind(profile: UserProfile) {
            nameView.text = profile.display_name ?: profile.username ?: "Unknown"
            usernameView.text = profile.username?.let { "@$it" } ?: ""
        }
    }
}


