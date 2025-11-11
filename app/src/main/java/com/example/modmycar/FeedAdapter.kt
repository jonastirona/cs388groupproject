package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import coil.load
import androidx.core.view.isVisible

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.VH>() {

    private val items = mutableListOf<Post>()

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
        holder.caption.text = post.caption ?: "(no caption)"
        holder.meta.text = "by ${post.userId} • ${post.likesCount} likes • ${post.commentsCount} comments"

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
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val caption: TextView = view.findViewById(R.id.tvCaption)
        val meta: TextView = view.findViewById(R.id.tvMeta)
    }
}
