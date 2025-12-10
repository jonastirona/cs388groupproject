package com.example.modmycar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

/**
 * Activity to display a friend's garage and their cars.
 * Clicking on a car will show the car detail with mods in read-only mode.
 */
class FriendGarageActivity : AppCompatActivity() {

    private lateinit var garageCarsRecyclerView: RecyclerView
    private lateinit var garageCarAdapter: GarageCarAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateTextView: TextView
    private lateinit var headerTextView: TextView

    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()
    private val carRepository: CarRepository = SupabaseCarRepository()

    private var friendId: String? = null
    private var friendName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_garage)

        // Get friend info from intent
        friendId = intent.getStringExtra(EXTRA_FRIEND_ID)
        friendName = intent.getStringExtra(EXTRA_FRIEND_NAME) ?: "Friend"

        if (friendId == null) {
            Toast.makeText(this, "Friend ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadFriendGarage()
    }

    private fun setupViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.friendGarageToolbar)
        toolbar.title = "$friendName's Garage"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        headerTextView = findViewById(R.id.friendGarageHeaderTextView)
        headerTextView.text = "Cars in $friendName's garage"

        loadingProgressBar = findViewById(R.id.friendGarageProgressBar)
        emptyStateTextView = findViewById(R.id.friendGarageEmptyState)

        garageCarsRecyclerView = findViewById(R.id.friendGarageCarsRecyclerView)
        garageCarsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize with empty adapter
        garageCarAdapter = GarageCarAdapter(emptyList()) { car ->
            // Navigate to car detail (will be read-only since viewer is not owner)
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
    }

    private fun loadFriendGarage() {
        val userId = friendId ?: return

        loadingProgressBar.visibility = View.VISIBLE
        emptyStateTextView.visibility = View.GONE
        garageCarsRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            // Load garage cars for the friend
            when (val garageCarsResult = garageCarRepository.getGarageCarsByUserId(userId)) {
                is AuthResult.Success -> {
                    val garageCars = garageCarsResult.data

                    if (garageCars.isEmpty()) {
                        loadingProgressBar.visibility = View.GONE
                        showEmptyState("$friendName hasn't added any cars to their garage yet.")
                        return@launch
                    }

                    // Load car details (make/model) for each garage car
                    when (val carsResult = carRepository.getAllCars()) {
                        is AuthResult.Success -> {
                            val carsMap = carsResult.data.associateBy { it.id }

                            val displayItems = garageCars.mapNotNull { garageCar ->
                                val car = carsMap[garageCar.carId]
                                if (car != null) {
                                    val imageUrl = CarImageService.getCarImageUrlSuspend(car.make)
                                    Log.d("FriendGarageActivity", "Car: ${car.make} ${car.model}, Image URL: $imageUrl")
                                    GarageCarDisplayItem(
                                        id = garageCar.id,
                                        carId = garageCar.carId,
                                        make = car.make,
                                        model = car.model,
                                        year = garageCar.year ?: 0,
                                        color = garageCar.color,
                                        imageUrl = imageUrl
                                    )
                                } else {
                                    Log.w("FriendGarageActivity", "Car not found for garageCar.carId: ${garageCar.carId}")
                                    null
                                }
                            }

                            loadingProgressBar.visibility = View.GONE

                            if (displayItems.isEmpty()) {
                                showEmptyState("$friendName hasn't added any cars to their garage yet.")
                            } else {
                                garageCarsRecyclerView.visibility = View.VISIBLE
                                garageCarAdapter = GarageCarAdapter(displayItems) { car ->
                                    val intent = Intent(this@FriendGarageActivity, CarDetailActivity::class.java).apply {
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
                        }
                        is AuthResult.Error -> {
                            loadingProgressBar.visibility = View.GONE
                            showEmptyState("Failed to load car details: ${carsResult.message}")
                        }
                    }
                }
                is AuthResult.Error -> {
                    loadingProgressBar.visibility = View.GONE
                    showEmptyState("Failed to load garage: ${garageCarsResult.message}")
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = View.VISIBLE
        garageCarsRecyclerView.visibility = View.GONE
    }

    companion object {
        const val EXTRA_FRIEND_ID = "extra_friend_id"
        const val EXTRA_FRIEND_NAME = "extra_friend_name"
    }
}

