package com.example.modmycar

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MaintenanceAdapter(
    private var items: List<MaintenanceItem>,
    private val onItemClicked: (MaintenanceItem) -> Unit
) : RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder>() {

    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    class MaintenanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: LinearLayout = itemView.findViewById(R.id.maintenanceItemLayout)
        val nameTextView: TextView = itemView.findViewById(R.id.maintenanceNameTextView)
        val statusBadge: TextView = itemView.findViewById(R.id.statusBadgeTextView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.maintenanceProgressBar)
        val lastServiceTextView: TextView = itemView.findViewById(R.id.lastServiceDateTextView)
        val nextDueTextView: TextView = itemView.findViewById(R.id.nextDueDateTextView)
        val daysRemainingTextView: TextView = itemView.findViewById(R.id.daysRemainingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_maintenance, parent, false)
        return MaintenanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        val item = items[position]
        val today = LocalDate.now()

        // Set display name from type
        holder.nameTextView.text = getDisplayName(item.type)

        // Calculate dates and status
        val lastServiceDate = item.lastServiceDate?.let {
            try {
                LocalDate.parse(it, isoDateFormatter)
            } catch (e: Exception) {
                null
            }
        }

        if (lastServiceDate != null) {
            val nextDueDate = lastServiceDate.plusDays(item.intervalDays.toLong())
            val daysRemaining = ChronoUnit.DAYS.between(today, nextDueDate).toInt()
            val daysSinceService = ChronoUnit.DAYS.between(lastServiceDate, today).toInt()

            // Calculate progress (0-100)
            val progress = ((daysSinceService.toFloat() / item.intervalDays) * 100).toInt().coerceIn(0, 100)
            holder.progressBar.progress = progress

            // Determine status
            val status = when {
                daysRemaining < 0 -> MaintenanceStatus.OVERDUE
                daysRemaining <= 14 -> MaintenanceStatus.DUE_SOON
                else -> MaintenanceStatus.OK
            }

            // Set status badge
            setStatusBadge(holder, status, daysRemaining)

            // Set progress bar color based on status
            setProgressBarColor(holder.progressBar, status)

            // Set date texts
            holder.lastServiceTextView.text = lastServiceDate.format(displayDateFormatter)
            holder.nextDueTextView.text = nextDueDate.format(displayDateFormatter)

            // Set days remaining text
            holder.daysRemainingTextView.text = when {
                daysRemaining < 0 -> "${-daysRemaining} days overdue"
                daysRemaining == 0 -> "Due today!"
                daysRemaining == 1 -> "1 day remaining"
                else -> "$daysRemaining days remaining"
            }
        } else {
            // No service logged yet
            holder.progressBar.progress = 0
            holder.statusBadge.text = "NOT SET"
            holder.statusBadge.background.setColorFilter(Color.parseColor("#9E9E9E"), PorterDuff.Mode.SRC_IN)
            holder.lastServiceTextView.text = "Not logged"
            holder.nextDueTextView.text = "—"
            holder.daysRemainingTextView.text = "Tap to log first service"
            setProgressBarColor(holder.progressBar, null)
        }

        // Click listener
        holder.itemLayout.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<MaintenanceItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun getDisplayName(type: String): String {
        return MaintenanceType.entries.find { it.typeName == type }?.displayName ?: type.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun setStatusBadge(holder: MaintenanceViewHolder, status: MaintenanceStatus, daysRemaining: Int) {
        val (text, color) = when (status) {
            MaintenanceStatus.OK -> "OK" to "#4CAF50"           // Green
            MaintenanceStatus.DUE_SOON -> "DUE SOON" to "#FF9800" // Orange
            MaintenanceStatus.OVERDUE -> "OVERDUE" to "#F44336"   // Red
        }
        holder.statusBadge.text = text
        holder.statusBadge.background.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN)
    }

    private fun setProgressBarColor(progressBar: ProgressBar, status: MaintenanceStatus?) {
        val color = when (status) {
            MaintenanceStatus.OK -> "#4CAF50"           // Green
            MaintenanceStatus.DUE_SOON -> "#FF9800"    // Orange
            MaintenanceStatus.OVERDUE -> "#F44336"     // Red
            null -> "#E0E0E0"                          // Gray (not set)
        }
        progressBar.progressDrawable.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN)
    }
}

