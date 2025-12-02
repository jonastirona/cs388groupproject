package com.example.modmycar

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Represents a selected media item with its URI and type.
 */
data class SelectedMedia(
    val uri: Uri,
    val mimeType: String
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
}

/**
 * Adapter for displaying selected media previews in CreatePostActivity.
 */
class MediaPreviewAdapter(
    private val context: Context,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaPreviewAdapter.ViewHolder>() {

    private val mediaItems = mutableListOf<SelectedMedia>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.mediaThumbnail)
        val videoIndicator: ImageView = view.findViewById(R.id.videoIndicator)
        val removeButton: ImageButton = view.findViewById(R.id.removeMediaButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val media = mediaItems[position]
        
        // Load thumbnail
        if (media.isVideo) {
            // For videos, load video thumbnail
            holder.thumbnail.setImageURI(media.uri)
            holder.videoIndicator.visibility = View.VISIBLE
        } else {
            // For images, load directly
            holder.thumbnail.setImageURI(media.uri)
            holder.videoIndicator.visibility = View.GONE
        }

        // Remove button click
        holder.removeButton.setOnClickListener {
            onRemoveClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = mediaItems.size

    fun addMedia(media: SelectedMedia) {
        mediaItems.add(media)
        notifyItemInserted(mediaItems.size - 1)
    }

    fun addAllMedia(mediaList: List<SelectedMedia>) {
        val startPos = mediaItems.size
        mediaItems.addAll(mediaList)
        notifyItemRangeInserted(startPos, mediaList.size)
    }

    fun removeAt(position: Int) {
        if (position in 0 until mediaItems.size) {
            mediaItems.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getMediaItems(): List<SelectedMedia> = mediaItems.toList()

    fun isEmpty(): Boolean = mediaItems.isEmpty()

    fun clear() {
        val size = mediaItems.size
        mediaItems.clear()
        notifyItemRangeRemoved(0, size)
    }
}

