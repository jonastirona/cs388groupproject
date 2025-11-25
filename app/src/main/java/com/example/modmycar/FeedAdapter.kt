package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import coil.load
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class FeedAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<FeedAdapter.VH>() {

    private val items = mutableListOf<Post>()
    private val likeRepository = SupabaseLikeRepository(com.example.modmycar.SupabaseClient.client)
    private val postRepository = try {
        SupabasePostRepository(com.example.modmycar.SupabaseClient.client)
    } catch (e: Exception) {
        null
    }

    fun setItems(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // Will swap to DiffUtil later
    }

    fun updatePostCounts(postId: String, likesCount: Int, commentsCount: Int) {
        val index = items.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val updated = items[index].copy(
                likesCount = likesCount,
                commentsCount = commentsCount
            )
            items[index] = updated
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        holder.caption.text = post.caption ?: "(no caption)"
        holder.meta.text = "by ${post.userId}"

        val firstImage = post.media.firstOrNull { it.type == "image" && it.url.isNotBlank() }?.url
        val imageView = holder.itemView.findViewById<ImageView>(R.id.postImage)

        if (firstImage != null) {
            imageView.visibility = View.VISIBLE
            imageView.load(firstImage) {
                crossfade(true)
            }
        } else {
            imageView.visibility = View.GONE
        }

        // Update like and comment counts
        holder.likeCount.text = post.likesCount.toString()
        holder.commentCount.text = post.commentsCount.toString()

        // Set up like button
        holder.likeButton.setOnClickListener {
            onLikeClick?.invoke(post) ?: toggleLike(post, holder, position)
        }

        // Set up comment button - navigate to detail
        holder.commentButton.setOnClickListener {
            onPostClick(post)
        }

        // Check if post is liked by current user
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val session = com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId != null) {
                    val isLiked = likeRepository.isLiked(post.id, userId)
                    holder.likeButton.text = if (isLiked) "Unlike" else "Like"
                }
            } catch (e: Exception) {
                // User not authenticated or error checking like status
            }
        }

        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    private fun toggleLike(post: Post, holder: VH, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val session = com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId == null) {
                    return@launch
                }

                val isLiked = likeRepository.isLiked(post.id, userId)
                if (isLiked) {
                    likeRepository.unlikePost(post.id, userId)
                    holder.likeButton.text = "Like"
                    val newCount = (post.likesCount - 1).coerceAtLeast(0)
                    holder.likeCount.text = newCount.toString()
                    items[position] = post.copy(likesCount = newCount)
                } else {
                    likeRepository.likePost(post.id, userId)
                    holder.likeButton.text = "Unlike"
                    val newCount = post.likesCount + 1
                    holder.likeCount.text = newCount.toString()
                    items[position] = post.copy(likesCount = newCount)
                }
            } catch (e: Exception) {
                // Error handling - could show a snackbar
            }
        }
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val caption: TextView = view.findViewById(R.id.tvCaption)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val likeButton: MaterialButton = view.findViewById(R.id.likeButton)
        val commentButton: MaterialButton = view.findViewById(R.id.commentButton)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val commentCount: TextView = view.findViewById(R.id.commentCount)
    }
}
