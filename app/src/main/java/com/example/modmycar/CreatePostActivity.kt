package com.example.modmycar

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

class CreatePostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        val toolbar = findViewById<MaterialToolbar>(R.id.createPostToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<Button>(R.id.uploadMediaButton).setOnClickListener {
            Snackbar.make(it, "Media uploads coming soon!", Snackbar.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.savePostButton).setOnClickListener {
            Snackbar.make(it, "Post creation coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }
}

