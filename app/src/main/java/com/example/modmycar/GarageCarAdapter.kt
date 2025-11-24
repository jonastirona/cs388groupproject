package com.example.modmycar

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

data class GarageCarDisplayItem(
    val id: String,
    val carId: String,
    val make: String,
    val model: String,
    val year: Int,
    val color: String?,
    val imageUrl: String? = null
)

class GarageCarAdapter(
    private val cars: List<GarageCarDisplayItem>,
    private val onCarClick: (GarageCarDisplayItem) -> Unit
) : RecyclerView.Adapter<GarageCarAdapter.GarageCarViewHolder>() {

    class GarageCarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val carPhotoImageView: ImageView = itemView.findViewById(R.id.carPhotoImageView)
        val carModelYearTextView: TextView = itemView.findViewById(R.id.carModelYearTextView)
        val colorBarView: View = itemView.findViewById(R.id.colorBarView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GarageCarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garage_car, parent, false)
        return GarageCarViewHolder(view)
    }

    override fun onBindViewHolder(holder: GarageCarViewHolder, position: Int) {
        val car = cars[position]
        
        // Set car model and year
        holder.carModelYearTextView.text = "${car.make} ${car.model} ${car.year}"
        
        // Set color bar
        val colorHex = car.color ?: "#CCCCCC"
        try {
            holder.colorBarView.setBackgroundColor(Color.parseColor(colorHex))
        } catch (e: Exception) {
            holder.colorBarView.setBackgroundColor(Color.parseColor("#CCCCCC"))
        }
        
        // Load car image with proper caching
        if (car.imageUrl != null) {
            Log.d("GarageCarAdapter", "Loading image for ${car.make} ${car.model} from URL: ${car.imageUrl}")
            holder.carPhotoImageView.load(car.imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
                // Enable memory and disk caching
                memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                diskCachePolicy(coil.request.CachePolicy.ENABLED)
                // Use a stable key based on make to ensure consistent caching
                memoryCacheKey(car.make)
                listener(
                    onStart = { request ->
                        Log.d("GarageCarAdapter", "Image load started for ${car.make}: ${request.data}")
                    },
                    onSuccess = { _, result ->
                        Log.d("GarageCarAdapter", "Image loaded successfully for ${car.make} ${car.model}")
                    },
                    onError = { _, result ->
                        Log.e("GarageCarAdapter", "Image load failed for ${car.make} ${car.model}: ${result.throwable.message}", result.throwable)
                    }
                )
            }
        } else {
            Log.w("GarageCarAdapter", "No image URL for ${car.make} ${car.model} - using placeholder")
            holder.carPhotoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        holder.itemView.setOnClickListener {
            onCarClick(car)
        }
    }

    override fun getItemCount(): Int = cars.size
}

