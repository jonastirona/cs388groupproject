package com.example.modmycar

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import coil.load
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import io.github.jan.supabase.auth.auth
class PostDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.postDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        // Get current user ID
        lifecycleScope.launch {
            try {
                val session = com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                userId?.let { viewModel.setCurrentUserId(it) }
            } catch (e: Exception) {
                // User not authenticated, but we can still show the post
            }
        }

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId == null) {
            finish()
            return
        }

        setupViews()
        setupObservers()
        viewModel.loadPost(postId)
    }

    private fun setupViews() {
        val likeButton = findViewById<MaterialButton>(R.id.likeButton)
        val commentInput = findViewById<EditText>(R.id.commentInput)
        val sendCommentButton = findViewById<MaterialButton>(R.id.sendCommentButton)
        val commentsRecyclerView = findViewById<RecyclerView>(R.id.commentsRecyclerView)

        likeButton.setOnClickListener {
            val currentUserId = try {
                com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
            } catch (e: Exception) {
                null
            }
            if (currentUserId != null) {
                viewModel.toggleLike()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please sign in to like posts", Snackbar.LENGTH_SHORT).show()
            }
        }

        sendCommentButton.setOnClickListener {
            val currentUserId = try {
                com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
            } catch (e: Exception) {
                null
            }
            if (currentUserId == null) {
                Snackbar.make(findViewById(android.R.id.content), "Please sign in to comment", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = commentInput.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.addComment(content)
                commentInput.text.clear()
            }
        }

        commentsAdapter = CommentsAdapter { comment ->
            // Allow deleting own comments
            val currentUserId = try {
                com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
            } catch (e: Exception) {
                null
            }
            if (comment.userId == currentUserId) {
                viewModel.deleteComment(comment.id)
            }
        }

        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.post.collect { post ->
                    post?.let { updatePostViews(it) }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.comments.collect { comments ->
                    commentsAdapter.setComments(comments)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLiked.collect { isLiked ->
                    val likeButton = findViewById<MaterialButton>(R.id.likeButton)
                    likeButton.text = if (isLiked) "Unlike" else "Like"
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun updatePostViews(post: Post) {
        val captionView = findViewById<TextView>(R.id.postCaption)
        val metaView = findViewById<TextView>(R.id.postMeta)
        val imageView = findViewById<ImageView>(R.id.postImage)
        val likeCountView = findViewById<TextView>(R.id.likeCount)
        val commentCountView = findViewById<TextView>(R.id.commentCount)

        captionView.text = post.caption ?: "(no caption)"
        metaView.text = "by ${post.userId}"

        val firstImage = post.media.firstOrNull { it.type == "image" && it.url.isNotBlank() }?.url
        if (firstImage != null) {
            imageView.visibility = View.VISIBLE
            imageView.load(firstImage) {
                crossfade(true)
            }
        } else {
            imageView.visibility = View.GONE
        }

        likeCountView.text = "${post.likesCount} likes"
        commentCountView.text = "${post.commentsCount} comments"
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }
}

// Comments Adapter
class CommentsAdapter(
    private val onCommentClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private val comments = mutableListOf<Comment>()

    fun setComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CommentViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
        holder.itemView.setOnClickListener {
            onCommentClick(comment)
        }
    }

    override fun getItemCount() = comments.size

    class CommentViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val contentView: TextView = itemView.findViewById(R.id.commentContent)
        private val userView: TextView = itemView.findViewById(R.id.commentUser)
        private val timeView: TextView = itemView.findViewById(R.id.commentTime)

        fun bind(comment: Comment) {
            contentView.text = comment.content
            userView.text = "User ${comment.userId.take(8)}"
            timeView.text = formatTime(comment.createdAt)
        }

        private fun formatTime(createdAt: String): String {
            return try {
                // Simple formatting - you can improve this with proper date parsing
                createdAt.take(10) // Just show date for now
            } catch (e: Exception) {
                ""
            }
        }
    }
}

