package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load

class NewsAdapter(
    private val onArticleClick: (NewsArticleSummary) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private val items = mutableListOf<NewsArticleSummary>()

    fun setItems(newItems: List<NewsArticleSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_article, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = items[position]
        holder.bind(article)
        holder.itemView.setOnClickListener { onArticleClick(article) }
    }

    override fun getItemCount(): Int = items.size

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.newsTitle)
        private val meta: TextView = view.findViewById(R.id.newsMeta)
        private val image: ImageView = view.findViewById(R.id.newsImage)

        fun bind(article: NewsArticleSummary) {
            title.text = article.title
            val source = article.source ?: "Unknown source"
            val published = article.publishedAt ?: ""
            meta.text = listOf(source, published).filter { it.isNotBlank() }.joinToString(" • ")

            if (!article.imageUrl.isNullOrBlank()) {
                image.isVisible = true
                image.load(article.imageUrl) {
                    crossfade(true)
                }
            } else {
                image.isVisible = false
            }
        }
    }
}

