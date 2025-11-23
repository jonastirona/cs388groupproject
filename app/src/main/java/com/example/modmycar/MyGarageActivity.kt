package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MyGarageActivity : AppCompatActivity() {

    private lateinit var garageCarsRecyclerView: RecyclerView
    private lateinit var garageCarAdapter: GarageCarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_garage)

        val toolbar = findViewById<MaterialToolbar>(R.id.garageToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        garageCarsRecyclerView = findViewById(R.id.garageCarsRecyclerView)
        garageCarsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // TODO: Replace with actual data from repository
        val placeholderCars = listOf(
            GarageCarDisplayItem(
                id = "1",
                carId = "car1",
                make = "Honda",
                model = "Civic Si",
                year = 2008,
                color = "#FF0000",
                imageUrl = null
            ),
            GarageCarDisplayItem(
                id = "2",
                carId = "car2",
                make = "Toyota",
                model = "Supra",
                year = 2020,
                color = "#0000FF",
                imageUrl = null
            )
        )
        
        garageCarAdapter = GarageCarAdapter(placeholderCars) { car ->
            val intent = Intent(this, CarDetailActivity::class.java).apply {
                putExtra("GARAGE_CAR_ID", car.id)
                putExtra("CAR_ID", car.carId)
                putExtra("CAR_MAKE", car.make)
                putExtra("CAR_MODEL", car.model)
                putExtra("CAR_YEAR", car.year)
                putExtra("CAR_COLOR", car.color)
            }
            startActivity(intent)
        }
        
        garageCarsRecyclerView.adapter = garageCarAdapter

        findViewById<FloatingActionButton>(R.id.addCarFAB).setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }
    }
}
