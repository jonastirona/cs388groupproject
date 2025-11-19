package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class MyGarageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_garage)

        val toolbar = findViewById<MaterialToolbar>(R.id.garageToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<Button>(R.id.viewSampleCarButton).setOnClickListener {
            startActivity(Intent(this, CarDetailActivity::class.java))
        }

        findViewById<Button>(R.id.addCarButton).setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }
    }
}

