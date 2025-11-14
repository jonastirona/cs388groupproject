package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val friendActionListener: FriendActionListener? = null
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private val searchResults = mutableListOf<SearchResultItem>()

    fun setSearchResults(results: List<SearchResultItem>) {
        searchResults.clear()
        searchResults.addAll(results)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val result = searchResults[position]
        val currentUserId = friendActionListener?.getCurrentUserId()
        val isCurrentUser = currentUserId == result.userId
        
        holder.bind(result, friendActionListener, isCurrentUser)
    }

    override fun getItemCount() = searchResults.size

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val usernameText: TextView = view.findViewById(R.id.tvSearchUsername)
        private val displayNameText: TextView = view.findViewById(R.id.tvSearchDisplayName)
        private val followButton: Button = view.findViewById(R.id.btnFollowSearch)

        fun bind(
            result: SearchResultItem,
            listener: FriendActionListener?,
            isCurrentUser: Boolean
        ) {
            usernameText.text = result.username ?: result.userId.take(8)
            displayNameText.text = result.displayName ?: ""

            if (isCurrentUser) {
                followButton.visibility = View.GONE
            } else {
                followButton.visibility = View.VISIBLE
                followButton.text = if (result.isFollowing) "Unfollow" else "Follow"
                followButton.setOnClickListener {
                    listener?.onFollowToggle(result.userId, result.isFollowing)
                }
            }
        }
    }
}

data class SearchResultItem(
    val userId: String,
    val username: String?,
    val displayName: String?,
    val isFollowing: Boolean = false
)


