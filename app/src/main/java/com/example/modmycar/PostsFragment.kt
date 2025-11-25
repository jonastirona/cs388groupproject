package com.example.modmycar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PostsFragment : Fragment(R.layout.fragment_posts) {

    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var adapter: FeedAdapter
    private val postDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val postId = data.getStringExtra(PostDetailActivity.EXTRA_POST_ID) ?: return@registerForActivityResult
        val likes = data.getIntExtra(PostDetailActivity.EXTRA_POST_LIKES, -1)
        val comments = data.getIntExtra(PostDetailActivity.EXTRA_POST_COMMENTS, -1)
        if (likes >= 0 && comments >= 0) {
            adapter.updatePostCounts(postId, likes, comments)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.postsRecyclerView)
        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.postsSwipeRefresh)
        val progressBar = view.findViewById<View>(R.id.postsProgressBar)

        adapter = FeedAdapter(onPostClick = { post ->
            val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            }
            postDetailLauncher.launch(intent)
        })

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val total = adapter.itemCount
                if (total == 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val threshold = 3
                if (lastVisible >= total - threshold) {
                    feedViewModel.loadNextPage()
                }
            }
        })

        swipeRefresh.setOnRefreshListener {
            feedViewModel.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.posts.collect { posts ->
                    adapter.setItems(posts)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.isLoading.collect { loading ->
                    progressBar.isVisible = loading && adapter.itemCount == 0
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.isRefreshing.collect { refreshing ->
                    swipeRefresh.isRefreshing = refreshing
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                feedViewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
                        feedViewModel.clearError()
                    }
                }
            }
        }

        feedViewModel.refresh()
    }
}

