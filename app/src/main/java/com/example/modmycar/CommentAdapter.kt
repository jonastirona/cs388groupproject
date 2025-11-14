package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val comments = mutableListOf<Comment>()

    fun setComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount() = comments.size

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val userText: TextView = view.findViewById(R.id.tvCommentUser)
        private val timeText: TextView = view.findViewById(R.id.tvCommentTime)
        private val contentText: TextView = view.findViewById(R.id.tvCommentContent)

        fun bind(comment: Comment) {
            userText.text = comment.userId.take(8) // Show first 8 chars of user ID
            contentText.text = comment.content

            // Format time
            val timeAgo = comment.createdAt?.let { formatTimeAgo(it) } ?: "now"
            timeText.text = timeAgo
        }

        private fun formatTimeAgo(createdAt: String): String {
            return try {
                val created = Instant.parse(createdAt)
                val now = Instant.now()
                val minutesAgo = ChronoUnit.MINUTES.between(created, now)
                val hoursAgo = ChronoUnit.HOURS.between(created, now)
                val daysAgo = ChronoUnit.DAYS.between(created, now)

                when {
                    minutesAgo < 1 -> "just now"
                    minutesAgo < 60 -> "${minutesAgo}m ago"
                    hoursAgo < 24 -> "${hoursAgo}h ago"
                    daysAgo < 7 -> "${daysAgo}d ago"
                    else -> {
                        val formatter = DateTimeFormatter.ofPattern("MMM d")
                        formatter.format(created)
                    }
                }
            } catch (e: Exception) {
                "recently"
            }
        }
    }
}


