package com.example.modmycar

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val feedViewModel: FeedViewModel by viewModels()
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Setup RecyclerView
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.feedRecycler)
        feedAdapter = FeedAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = feedAdapter

        // Load more data when reaching the end
        val layoutManager = rv.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
        rv.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return  // only care about scroll-down

                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val threshold = 3  // start loading when 3 items from the end

                if (lastVisible >= total - threshold) {
                    feedViewModel.loadMore(total)  // ask for the next page
                }
            }
        })

        // Start loading data
        feedViewModel.loadInitial()

        // Log posts count
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                feedViewModel.posts.collect { posts ->
                    feedAdapter.setItems(posts)      // Update UI
                    Log.d("FeedVM", "Rendered ${posts.size} posts")
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}