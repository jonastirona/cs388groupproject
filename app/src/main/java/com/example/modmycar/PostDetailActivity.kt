package com.example.modmycar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class PostDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: PostDetailViewModel
    private lateinit var commentsAdapter: CommentsAdapter

    private var exoPlayer: ExoPlayer? = null
    private var currentAudioUrl: String? = null
    private var isAudioPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.postDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        viewModel = ViewModelProvider(this)[PostDetailViewModel::class.java]

        lifecycleScope.launch {
            try {
                val session = com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                userId?.let { viewModel.setCurrentUserId(it) }
            } catch (e: Exception) {
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
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Please sign in to like posts",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        sendCommentButton.setOnClickListener {
            val currentUserId = try {
                com.example.modmycar.SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
            } catch (e: Exception) {
                null
            }
            if (currentUserId == null) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Please sign in to comment",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val content = commentInput.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.addComment(content)
                commentInput.text.clear()
            }
        }

        commentsAdapter = CommentsAdapter { comment ->
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
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            it,
                            Snackbar.LENGTH_LONG
                        ).show()
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

        val audioPreview = findViewById<View>(R.id.audioPreviewContainer)
        val audioIcon = findViewById<ImageView>(R.id.audioIcon)

        captionView.text = post.caption ?: "(no caption)"
        val displayName = post.authorProfile?.displayName
            ?: post.authorProfile?.username
            ?: post.userId.take(8)

        metaView.text = "by $displayName"

        val videoContainer = findViewById<View>(R.id.videoPlayerContainer)
        val videoView = findViewById<PlayerView>(R.id.videoPlayerView)
        val videoUrl = post.media.firstOrNull { it.type == "video" && it.url.isNotBlank() }?.url

        if (videoUrl != null) {
            videoContainer.visibility = View.VISIBLE
            imageView.visibility = View.GONE

            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this).build()
            }

            videoView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.play()
        } else {
            videoContainer.visibility = View.GONE
        }

        val audioUrl = post.media.firstOrNull { it.type == "audio" && it.url.isNotBlank() }?.url
        if (audioUrl != null) {
            audioPreview.visibility = View.VISIBLE

            if (audioUrl == currentAudioUrl && isAudioPlaying) {
                audioIcon.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                audioIcon.setImageResource(android.R.drawable.ic_media_play)
            }

            audioPreview.setOnClickListener {
                handleAudioPlayback(audioUrl, audioIcon)
            }
        } else {
            audioPreview.visibility = View.GONE
        }

        val firstImage = post.media.firstOrNull { it.type == "image" && it.url.isNotBlank() }?.url
        if (firstImage != null && videoUrl == null) {
            imageView.visibility = View.VISIBLE
            imageView.load(firstImage) {
                crossfade(true)
            }
        } else if (videoUrl == null) {
            imageView.visibility = View.GONE
        }

        likeCountView.text = "${post.likesCount} likes"
        commentCountView.text = "${post.commentsCount} comments"
    }

    private fun handleAudioPlayback(url: String, icon: ImageView) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
        }

        val player = exoPlayer!!

        if (currentAudioUrl == url) {
            if (player.isPlaying) {
                player.pause()
                isAudioPlaying = false
                icon.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.play()
                isAudioPlaying = true
                icon.setImageResource(android.R.drawable.ic_media_pause)
            }
            return
        }

        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        currentAudioUrl = url
        isAudioPlaying = true
        icon.setImageResource(android.R.drawable.ic_media_pause)
    }

    override fun finish() {
        val currentPost = viewModel.post.value
        if (currentPost != null) {
            val data = Intent().apply {
                putExtra(EXTRA_POST_ID, currentPost.id)
                putExtra(EXTRA_POST_LIKES, currentPost.likesCount)
                putExtra(EXTRA_POST_COMMENTS, currentPost.commentsCount)
            }
            setResult(Activity.RESULT_OK, data)
        }
        super.finish()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_POST_LIKES = "extra_post_likes"
        const val EXTRA_POST_COMMENTS = "extra_post_comments"
    }
}

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
            userView.text = comment.authorProfile?.displayName
                ?: comment.authorProfile?.username
                        ?: "User ${comment.userId.take(8)}"
            timeView.text = formatTime(comment.createdAt)
        }

        private fun formatTime(createdAt: String): String {
            return try {
                createdAt.take(10)
            } catch (e: Exception) {
                ""
            }
        }
    }
}