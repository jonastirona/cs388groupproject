package com.example.modmycar

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import coil.load
import androidx.core.view.isVisible
import coil.request.videoFrameMillis
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth


class FeedAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<FeedAdapter.VH>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPostId: String? = null

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
        holder.itemView.tag = post.id

        val imageView = holder.itemView.findViewById<ImageView>(R.id.postImage)
        val audioPreview = holder.itemView.findViewById<View>(R.id.audioPreviewContainer)
        val audioIcon = holder.itemView.findViewById<ImageView>(R.id.audioIcon)
        val videoPreview = holder.itemView.findViewById<View>(R.id.videoPreviewContainer)
        val videoThumbnail = holder.itemView.findViewById<ImageView>(R.id.videoThumbnail)
        val videoPlayIcon = holder.itemView.findViewById<ImageView>(R.id.videoPlayIcon)

        val hasImage = post.media.any { it.type == "image" && it.url.isNotBlank() }
        val hasAudio = post.media.any { it.type == "audio" && it.url.isNotBlank() }
        val hasVideo = post.media.any { it.type == "video" && it.url.isNotBlank() }

        val firstImage = post.media.firstOrNull { it.type == "image" }?.url
        val audioUrl = post.media.firstOrNull { it.type == "audio" }?.url
        val videoUrl = post.media.firstOrNull { it.type == "video" }?.url

        imageView.visibility = View.GONE
        audioPreview.visibility = View.GONE
        videoPreview.visibility = View.GONE

        if (hasVideo && videoUrl != null) {
            videoPreview.visibility = View.VISIBLE
            videoThumbnail.load(videoUrl) {
                crossfade(true)
                videoFrameMillis(1000L)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }
            videoPreview.setOnClickListener {
                onPostClick(post)
            }
        } else if (hasAudio && audioUrl != null) {
            audioPreview.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            if (post.id == currentlyPlayingPostId) {
                audioIcon.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                audioIcon.setImageResource(android.R.drawable.ic_media_play)
            }
            audioPreview.setOnClickListener {
                handleAudioPlayback(post.id, audioUrl, audioIcon)
            }
        } else if (hasImage && firstImage != null) {
            imageView.visibility = View.VISIBLE
            imageView.load(firstImage) { crossfade(true) }

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
                val optimistic = if (isLiked) {
                    val newCount = (post.likesCount - 1).coerceAtLeast(0)
                    post.copy(likesCount = newCount)
                } else {
                    post.copy(likesCount = post.likesCount + 1)
                }
                items[position] = optimistic
                holder.likeCount.text = optimistic.likesCount.toString()

                if (isLiked) {
                    likeRepository.unlikePost(post.id, userId)
                    holder.likeButton.text = "Like"
                } else {
                    likeRepository.likePost(post.id, userId)
                    holder.likeButton.text = "Unlike"
                }

                val refreshed = postRepository?.let {
                    withContext(Dispatchers.IO) { it.getPost(post.id) }
                }
                val updated = refreshed ?: optimistic
                items[position] = updated
                holder.likeCount.text = updated.likesCount.toString()
                holder.commentCount.text = updated.commentsCount.toString()
            } catch (e: Exception) {
            }
        }
    }

    private fun handleAudioPlayback(postId: String, url: String, icon: ImageView) {
        if (mediaPlayer != null && currentlyPlayingPostId == postId) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                icon.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer!!.start()
                icon.setImageResource(android.R.drawable.ic_media_pause)
            }
            return
        }
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                icon.setImageResource(android.R.drawable.ic_media_pause)
            }
            setOnCompletionListener {
                icon.setImageResource(android.R.drawable.ic_media_play)
                currentlyPlayingPostId = null
                release()
                mediaPlayer = null
            }
            prepareAsync()
        }
        currentlyPlayingPostId = postId
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        val tag = holder.itemView.tag
        if (tag == currentlyPlayingPostId) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingPostId = null
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
