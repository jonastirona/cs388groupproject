package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

class CarDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.carDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<Button>(R.id.viewModTreeButton).setOnClickListener {
            startActivity(Intent(this, ModTreeActivity::class.java))
        }

        findViewById<Button>(R.id.addModButton).setOnClickListener {
            startActivity(Intent(this, AddModActivity::class.java))
        }

        findViewById<Button>(R.id.addPhotosButton).setOnClickListener {
            Snackbar.make(it, "Media upload placeholder", Snackbar.LENGTH_SHORT).show()
        }
    }
}

