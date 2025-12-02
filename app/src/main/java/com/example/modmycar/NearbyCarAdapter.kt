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
import kotlin.math.roundToInt

data class NearbyCarDisplayItem(
    val id: String,
    val carId: String,
    val make: String,
    val model: String,
    val year: Int,
    val color: String?,
    val imageUrl: String? = null,
    val ownerName: String?,
    val distanceMiles: Double
)

class NearbyCarAdapter(
    private var cars: List<NearbyCarDisplayItem>,
    private val onCarClick: (NearbyCarDisplayItem) -> Unit
) : RecyclerView.Adapter<NearbyCarAdapter.NearbyCarViewHolder>() {

    class NearbyCarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val carPhotoImageView: ImageView = itemView.findViewById(R.id.carPhotoImageView)
        val carModelYearTextView: TextView = itemView.findViewById(R.id.carModelYearTextView)
        val ownerNameTextView: TextView = itemView.findViewById(R.id.ownerNameTextView)
        val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)
        val colorBarView: View = itemView.findViewById(R.id.colorBarView)
    }
    
    fun updateCars(newCars: List<NearbyCarDisplayItem>) {
        cars = newCars
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyCarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_car, parent, false)
        return NearbyCarViewHolder(view)
    }

    override fun onBindViewHolder(holder: NearbyCarViewHolder, position: Int) {
        val car = cars[position]
        
        // Set car model and year
        val yearShort = if (car.year > 0) "'${car.year.toString().takeLast(2)}" else ""
        holder.carModelYearTextView.text = "${car.make} ${car.model} $yearShort"
        
        // Set owner name
        val ownerDisplayName = car.ownerName ?: "Unknown"
        holder.ownerNameTextView.text = "Owner: $ownerDisplayName"
        
        // Set distance
        val distanceText = when {
            car.distanceMiles < 0.1 -> "${(car.distanceMiles * 5280).roundToInt()} ft away"
            car.distanceMiles < 1.0 -> String.format("%.2f mi away", car.distanceMiles)
            else -> String.format("%.1f mi away", car.distanceMiles)
        }
        holder.distanceTextView.text = distanceText
        
        // Set color bar
        val colorHex = car.color ?: "#CCCCCC"
        try {
            holder.colorBarView.setBackgroundColor(Color.parseColor(colorHex))
        } catch (e: Exception) {
            holder.colorBarView.setBackgroundColor(Color.parseColor("#CCCCCC"))
        }
        
        // Load car image with proper caching
        if (car.imageUrl != null) {
            Log.d("NearbyCarAdapter", "Loading image for ${car.make} ${car.model} from URL: ${car.imageUrl}")
            holder.carPhotoImageView.load(car.imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
                memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                diskCachePolicy(coil.request.CachePolicy.ENABLED)
                memoryCacheKey(car.make)
            }
        } else {
            Log.w("NearbyCarAdapter", "No image URL for ${car.make} ${car.model} - using placeholder")
            holder.carPhotoImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        holder.itemView.setOnClickListener {
            onCarClick(car)
        }
    }

    override fun getItemCount(): Int = cars.size
}

