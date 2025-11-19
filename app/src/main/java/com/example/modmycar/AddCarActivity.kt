package com.example.modmycar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar

class AddCarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        val toolbar = findViewById<MaterialToolbar>(R.id.addCarToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<android.view.View>(R.id.saveCarButton).setOnClickListener {
            Snackbar.make(it, "Car creation placeholder", Snackbar.LENGTH_SHORT).show()
        }
    }
}

