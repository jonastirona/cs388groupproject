package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MyGarageActivity : AppCompatActivity() {

    private lateinit var garageCarsRecyclerView: RecyclerView
    private lateinit var garageCarAdapter: GarageCarAdapter
    
    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()
    private val carRepository: CarRepository = SupabaseCarRepository()
    private val authRepository: AuthRepository = SupabaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_garage)

        val toolbar = findViewById<MaterialToolbar>(R.id.garageToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        garageCarsRecyclerView = findViewById(R.id.garageCarsRecyclerView)
        garageCarsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        garageCarAdapter = GarageCarAdapter(emptyList()) { car ->
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
        
        loadGarageCars()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload cars when returning to this activity (e.g., after adding a new car)
        loadGarageCars()
    }
    
    private fun loadGarageCars() {
        lifecycleScope.launch {
            // Get current user
            val userResult = authRepository.getCurrentSession()
            val userId = when (userResult) {
                is AuthResult.Success -> userResult.data?.id
                is AuthResult.Error -> null
            }
            
            if (userId == null) {
                Toast.makeText(this@MyGarageActivity, "Please sign in to view your garage", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Load garage cars for this user
            when (val garageCarsResult = garageCarRepository.getGarageCarsByUserId(userId)) {
                is AuthResult.Success -> {
                    val garageCars = garageCarsResult.data
                    
                    // Load car details (make/model) for each garage car
                    when (val carsResult = carRepository.getAllCars()) {
                        is AuthResult.Success -> {
                            val carsMap = carsResult.data.associateBy { it.id }
                            
                            val displayItems = garageCars.mapNotNull { garageCar ->
                                val car = carsMap[garageCar.carId]
                                if (car != null) {
                                    GarageCarDisplayItem(
                                        id = garageCar.id,
                                        carId = garageCar.carId,
                                        make = car.make,
                                        model = car.model,
                                        year = garageCar.year ?: 0,
                                        color = garageCar.color,
                                        imageUrl = null
                                    )
                                } else {
                                    null
                                }
                            }
                            
                            garageCarAdapter = GarageCarAdapter(displayItems) { car ->
                                val intent = Intent(this@MyGarageActivity, CarDetailActivity::class.java).apply {
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
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(
                                this@MyGarageActivity,
                                "Failed to load car details: ${carsResult.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@MyGarageActivity,
                        "Failed to load garage cars: ${garageCarsResult.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
