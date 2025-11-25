package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class CommunityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        val toolbar = findViewById<MaterialToolbar>(R.id.communityToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<android.view.View>(R.id.communityFriends).setOnClickListener {
            startActivity(Intent(this, FriendsListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.communityFindFriends).setOnClickListener {
            startActivity(Intent(this, FriendSearchActivity::class.java))
        }
    }
}

