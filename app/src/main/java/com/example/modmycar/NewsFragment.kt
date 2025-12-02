package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NewsFragment : Fragment(R.layout.fragment_news) {

    private val newsViewModel: NewsViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: NewsAdapter

    private var appliedUserInterests = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.newsRecyclerView)
        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.newsSwipeRefresh)
        val loadingContainer = view.findViewById<View>(R.id.newsLoadingContainer)
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
                val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
                val total = adapter.itemCount
                if (total == 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val threshold = 3
                if (lastVisible >= total - threshold) {
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
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsViewModel.isLoading.collect { isLoading ->
                    val hasArticles = adapter.itemCount > 0
                    loadingContainer.isVisible = isLoading && !hasArticles
                    emptyView.isVisible = !isLoading && !hasArticles
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

        // Observe the authenticated user and, once available, build interest keywords from their
        // posts, cars, mods, and liked posts. This runs once per fragment lifecycle.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    if (user != null && !appliedUserInterests) {
                        appliedUserInterests = true
                        viewLifecycleOwner.lifecycleScope.launch {
                            val interests = buildUserInterestKeywords(user.id)
                            if (interests.isNotEmpty()) {
                                newsViewModel.setUserKeywords(interests)
                            }
                            newsViewModel.refresh()
                        }
                    } else if (user == null && !appliedUserInterests) {
                        // No authenticated user; just load the default feed once.
                        appliedUserInterests = true
        newsViewModel.refresh()
                    }
                }
            }
        }
    }

    /**
     * Collects raw interest strings for the given user from several sources:
     * - Posts they created
     * - Posts they liked
     * - Cars in their garage
     * - Mods they've completed
     *
     * The heavy work of intersecting these with the keyword bank is done inside
     * [NewsApiRepository]; here we just provide rich, descriptive text.
     */
    private suspend fun buildUserInterestKeywords(userId: String): List<String> {
        val client = SupabaseClient.client

        val postRepository: PostRepository = try {
            SupabasePostRepository(client)
        } catch (_: Exception) {
            LocalPostRepository()
        }
        val likeRepository: LikeRepository = SupabaseLikeRepository(client)
        val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository(client)
        val carRepository: CarRepository = SupabaseCarRepository(client)
        val garageModRepository: GarageModRepository = SupabaseGarageModRepository(client)
        val modRepository: ModRepository = SupabaseModRepository(client)

        val rawInterests = mutableListOf<String>()

        // 1) Posts created by the user (captions/descriptions)
        try {
            val allPosts = runCatching { postRepository.getFeed(limit = 100, offset = 0) }.getOrNull().orEmpty()
            val userPosts = allPosts.filter { it.userId == userId }
            userPosts.forEach { post ->
                post.caption?.let { rawInterests.add(it) }
                post.description?.let { rawInterests.add(it) }
            }
        } catch (_: Exception) {
            // Best-effort; ignore on failure.
        }

        // 2) Posts liked by the user
        try {
            val likes = runCatching { likeRepository.getLikesByUser(userId) }.getOrNull().orEmpty()
            likes
                .take(50) // avoid too many round trips
                .forEach { like ->
                    runCatching { postRepository.getPost(like.postId) }
                        .onSuccess { likedPost ->
                            likedPost.caption?.let { rawInterests.add(it) }
                            likedPost.description?.let { rawInterests.add(it) }
                        }
                }
        } catch (_: Exception) {
            // Ignore; personalization is best-effort.
        }

        // 3) Cars in the user's garage (make/model)
        try {
            val garageCarsResult = runCatching { garageCarRepository.getGarageCarsByUserId(userId) }.getOrNull()
            val garageCars = (garageCarsResult as? AuthResult.Success)?.data.orEmpty()
            garageCars.forEach { garageCar ->
                val carResult = runCatching { carRepository.getCar(garageCar.carId) }.getOrNull()
                val car = (carResult as? AuthResult.Success)?.data
                if (car != null) {
                    rawInterests.add("${car.make} ${car.model}")
                }
            }
        } catch (_: Exception) {
            // Ignore; fall back to other sources.
        }

        // 4) Mods the user has completed (names, categories, descriptions)
        try {
            val garageModsResult = runCatching { garageModRepository.getGarageModsByUserId(userId) }.getOrNull()
            val garageMods = (garageModsResult as? AuthResult.Success)?.data.orEmpty()
            garageMods.forEach { garageMod ->
                val modResult = runCatching { modRepository.getMod(garageMod.modId) }.getOrNull()
                val mod = (modResult as? AuthResult.Success)?.data
                if (mod != null) {
                    rawInterests.add(mod.name)
                    mod.category?.let { rawInterests.add(it) }
                    mod.description?.let { rawInterests.add(it) }
                }
            }
        } catch (_: Exception) {
            // Ignore.
        }

        // Deduplicate and trim to a reasonable size.
        return rawInterests
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(200)
    }
}

