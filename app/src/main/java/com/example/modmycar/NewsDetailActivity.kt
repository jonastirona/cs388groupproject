package com.example.modmycar

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NewsDetailActivity : AppCompatActivity() {

    private val viewModel: NewsDetailViewModel by viewModels()

    private lateinit var titleView: TextView
    private lateinit var sourceView: TextView
    private lateinit var contentView: TextView
    private lateinit var imageView: ImageView
    private lateinit var progressBar: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.newsDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        titleView = findViewById(R.id.newsDetailTitle)
        sourceView = findViewById(R.id.newsDetailSource)
        contentView = findViewById(R.id.newsDetailContent)
        imageView = findViewById(R.id.newsDetailImage)
        progressBar = findViewById(R.id.newsDetailProgress)

        val articleUrl = intent.getStringExtra(EXTRA_ARTICLE_URL)
        val titleHint = intent.getStringExtra(EXTRA_ARTICLE_TITLE)
        val imageHint = intent.getStringExtra(EXTRA_ARTICLE_IMAGE)
        val source = intent.getStringExtra(EXTRA_ARTICLE_SOURCE)
        val publishedAt = intent.getStringExtra(EXTRA_ARTICLE_PUBLISHED_AT)

        if (articleUrl.isNullOrBlank()) {
            Snackbar.make(titleView, "Missing article URL", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        observeViewModel()
        viewModel.loadArticle(articleUrl, titleHint, imageHint, source, publishedAt)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.article.collect { article ->
                    article?.let {
                        titleView.text = it.title
                        val metaParts = listOfNotNull(it.source, it.publishedAt, it.author)
                        sourceView.text = metaParts.joinToString(" • ")
                        contentView.text = it.content

                        if (!it.imageUrl.isNullOrBlank()) {
                            imageView.isVisible = true
                            imageView.load(it.imageUrl) {
                                crossfade(true)
                            }
                        } else {
                            imageView.isVisible = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    progressBar.isVisible = loading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(titleView, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ARTICLE_TITLE = "extra_article_title"
        const val EXTRA_ARTICLE_URL = "extra_article_url"
        const val EXTRA_ARTICLE_IMAGE = "extra_article_image"
        const val EXTRA_ARTICLE_SOURCE = "extra_article_source"
        const val EXTRA_ARTICLE_PUBLISHED_AT = "extra_article_published_at"
    }
}

