package com.example.modmycar

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

abstract class SimplePlaceholderActivity : AppCompatActivity() {

    abstract val screenTitle: String
    abstract val descriptionText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)

        val toolbar = findViewById<MaterialToolbar>(R.id.placeholderToolbar)
        toolbar.title = screenTitle
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val content = findViewById<TextView>(R.id.placeholderContent)
        content.text = descriptionText
    }
}

