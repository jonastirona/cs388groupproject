package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import coil.load
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText

interface CommentActionListener {
    fun onToggleComments(postId: String, isExpanded: Boolean)
    fun onPostComment(postId: String, content: String)
    fun getCommentsForPost(postId: String): List<Comment>
    fun getCurrentUserId(): String?
}

interface LikeActionListener {
    fun onLikeToggle(postId: String)
    fun isPostLiked(postId: String): Boolean
    fun getLikeCount(postId: String): Int
}

class FeedAdapter(
    private val commentActionListener: CommentActionListener? = null,
    private val likeActionListener: LikeActionListener? = null
) : RecyclerView.Adapter<FeedAdapter.VH>() {

    private val items = mutableListOf<Post>()
    private val expandedPosts = mutableSetOf<String>()

    fun setItems(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // Will swap to DiffUtil later
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        val currentUserId = commentActionListener?.getCurrentUserId()
        holder.bind(
            post, 
            expandedPosts.contains(post.id), 
            commentActionListener, 
            likeActionListener,
            currentUserId
        )
    }

    override fun getItemCount() = items.size

    fun toggleComments(postId: String) {
        if (expandedPosts.contains(postId)) {
            expandedPosts.remove(postId)
        } else {
            expandedPosts.add(postId)
        }
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val caption: TextView = view.findViewById(R.id.tvCaption)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val commentsSection: View = view.findViewById(R.id.commentsSection)
        val btnToggleComments: Button = view.findViewById(R.id.btnToggleComments)
        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.commentsRecyclerView)
        val etCommentInput: TextInputEditText = view.findViewById(R.id.etCommentInput)
        val btnPostComment: Button = view.findViewById(R.id.btnPostComment)

        private val commentAdapter = CommentAdapter()

        init {
            commentsRecyclerView.layoutManager = LinearLayoutManager(view.context)
            commentsRecyclerView.adapter = commentAdapter
        }

        fun bind(
            post: Post,
            isExpanded: Boolean,
            commentActionListener: CommentActionListener?,
            likeActionListener: LikeActionListener?,
            currentUserId: String?
        ) {
            caption.text = post.caption ?: "(no caption)"
            
            // Check if this is an RSS post (RSS posts can't be liked)
            val isRssPost = post.id.startsWith("rss-")
            
            // Update like button state
            val isLiked = if (!isRssPost) {
                likeActionListener?.isPostLiked(post.id) ?: false
            } else {
                false // RSS posts can't be liked
            }
            // Use ViewModel count if available, otherwise fall back to post's likesCount
            val viewModelCount = likeActionListener?.getLikeCount(post.id)
            val likeCount = if (viewModelCount != null && viewModelCount > 0) {
                viewModelCount
            } else {
                post.likesCount
            }
            
            // Set like button icon and color (using heart-like appearance)
            // Disable and gray out for RSS posts
            if (isRssPost) {
                btnLike.isEnabled = false
                btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                btnLike.alpha = 0.5f // Make it appear disabled
                btnLike.imageTintList = ContextCompat.getColorStateList(
                    itemView.context,
                    android.R.color.darker_gray
                )
            } else {
                btnLike.isEnabled = true
                btnLike.alpha = 1.0f
                if (isLiked) {
                    btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                    btnLike.imageTintList = ContextCompat.getColorStateList(
                        itemView.context,
                        android.R.color.holo_red_dark
                    )
                } else {
                    btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                    btnLike.imageTintList = ContextCompat.getColorStateList(
                        itemView.context,
                        android.R.color.darker_gray
                    )
                }
            }
            
            // Setup like button click (only for non-RSS posts)
            if (isRssPost) {
                btnLike.setOnClickListener(null)
            } else {
                btnLike.setOnClickListener {
                    likeActionListener?.onLikeToggle(post.id)
                }
            }
            
            meta.text = "by ${post.userId} • $likeCount likes • ${post.commentsCount} comments"

            val firstImage = post.media.firstOrNull { it.type == "image" && it.url.isNotBlank() }?.url
            val imageView = itemView.findViewById<ImageView>(R.id.postImage)

            if (firstImage != null) {
                imageView.visibility = View.VISIBLE
                imageView.load(firstImage) {
                    crossfade(true)
                }
            } else {
                imageView.visibility = View.GONE
            }

            // Setup comments section
            btnToggleComments.text = if (isExpanded) "Hide Comments" else "View Comments (${post.commentsCount})"
            commentsSection.visibility = if (isExpanded) View.VISIBLE else View.GONE

            btnToggleComments.setOnClickListener {
                commentActionListener?.onToggleComments(post.id, !isExpanded)
            }

            // Load and display comments if expanded
            if (isExpanded && commentActionListener != null) {
                val comments = commentActionListener.getCommentsForPost(post.id)
                commentAdapter.setComments(comments)
            } else {
                // Clear comments when collapsed to save memory
                commentAdapter.setComments(emptyList())
            }

            // Setup post comment button
            btnPostComment.setOnClickListener {
                val content = etCommentInput.text?.toString()?.trim()
                android.util.Log.d("FeedAdapter", "Post button clicked: content='$content', currentUserId=$currentUserId, listener=${commentActionListener != null}")
                if (content.isNullOrBlank()) {
                    android.util.Log.w("FeedAdapter", "Comment content is blank")
                    return@setOnClickListener
                }
                if (currentUserId == null) {
                    android.util.Log.e("FeedAdapter", "Cannot post comment: currentUserId is null")
                    return@setOnClickListener
                }
                if (commentActionListener == null) {
                    android.util.Log.e("FeedAdapter", "Cannot post comment: commentActionListener is null")
                    return@setOnClickListener
                }
                android.util.Log.d("FeedAdapter", "Calling onPostComment for post ${post.id}")
                commentActionListener.onPostComment(post.id, content)
                etCommentInput.text?.clear()
            }
        }
    }
}
