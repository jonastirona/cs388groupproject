package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class CarMediaAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<CarMediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.mediaImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        holder.imageView.load(imageUrl) {
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}

