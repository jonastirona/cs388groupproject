package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NewsFragment : Fragment(R.layout.fragment_news) {

    private val newsViewModel: NewsViewModel by viewModels()
    private lateinit var adapter: NewsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.newsRecyclerView)
        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.newsSwipeRefresh)
        val progressBar = view.findViewById<View>(R.id.newsProgressBar)
        val emptyView = view.findViewById<View>(R.id.newsEmptyState)

        adapter = NewsAdapter { article ->
            val intent = Intent(requireContext(), NewsDetailActivity::class.java).apply {
                putExtra(NewsDetailActivity.EXTRA_ARTICLE_TITLE, article.title)
                putExtra(NewsDetailActivity.EXTRA_ARTICLE_URL, article.link)
                putExtra(NewsDetailActivity.EXTRA_ARTICLE_IMAGE, article.imageUrl)
                putExtra(NewsDetailActivity.EXTRA_ARTICLE_SOURCE, article.source)
                putExtra(NewsDetailActivity.EXTRA_ARTICLE_PUBLISHED_AT, article.publishedAt)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
                val total = adapter.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= total - 4) {
                    newsViewModel.loadNextPage()
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            newsViewModel.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsViewModel.articles.collect { articles ->
                    adapter.setItems(articles)
                    emptyView.isVisible = articles.isEmpty() && !newsViewModel.isLoading.value
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsViewModel.isLoading.collect { isLoading ->
                    progressBar.isVisible = isLoading && adapter.itemCount == 0
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsViewModel.isRefreshing.collect { refreshing ->
                    swipeRefresh.isRefreshing = refreshing
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsViewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
                        newsViewModel.clearError()
                    }
                }
            }
        }

        newsViewModel.refresh()
    }
}

