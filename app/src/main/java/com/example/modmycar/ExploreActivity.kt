package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ExploreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore)

        val toolbar = findViewById<MaterialToolbar>(R.id.exploreToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<android.view.View>(R.id.openPopularBuilds).setOnClickListener {
            startActivity(Intent(this, PopularBuildsActivity::class.java))
        }


    }
}

