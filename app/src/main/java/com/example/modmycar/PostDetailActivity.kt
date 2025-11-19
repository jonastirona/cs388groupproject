package com.example.modmycar

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class PostDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.postDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val postId = intent.getStringExtra(EXTRA_POST_ID) ?: "Unknown"
        val contentView = findViewById<TextView>(R.id.postDetailContent)
        contentView.text = "Post details for ID: $postId\n\n" +
            "Future versions of this screen will show the photo carousel, likes/comments, and options to share."
    }

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }
}

