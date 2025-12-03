package com.example.modmycar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class PopularBuildsActivity : AppCompatActivity() {

    private lateinit var popularBuildsRecyclerView: RecyclerView
    private lateinit var nearbyCarAdapter: NearbyCarAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyStateTextView: TextView
    
    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()
    private val carRepository: CarRepository = SupabaseCarRepository()
    private val authRepository: AuthRepository = SupabaseAuthRepository()
    private val locationTrackingService: LocationTrackingService by lazy {
        LocationTrackingService(this)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                loadNearbyCars()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                loadNearbyCars()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Location permission is required to find nearby builds",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState("Location permission is required to find nearby builds.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_builds)

        val toolbar = findViewById<MaterialToolbar>(R.id.popularBuildsToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        popularBuildsRecyclerView = findViewById(R.id.popularBuildsRecyclerView)
        popularBuildsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        
        nearbyCarAdapter = NearbyCarAdapter(emptyList()) { car ->
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
        
        popularBuildsRecyclerView.adapter = nearbyCarAdapter

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadNearbyCars()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun loadNearbyCars() {
        loadingProgressBar.visibility = View.VISIBLE
        emptyStateTextView.visibility = View.GONE
        popularBuildsRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            // Update current user's location first
            locationTrackingService.updateUserLocation()
            
            // Get current location
            val currentLocation = locationTrackingService.getCurrentLocation()
            if (currentLocation == null) {
                loadingProgressBar.visibility = View.GONE
                showEmptyState("Unable to get your location. Please ensure location services are enabled.")
                return@launch
            }

            val (latitude, longitude) = currentLocation

            // Get current user ID to exclude own cars
            val userResult = authRepository.getCurrentSession()
            val currentUserId = when (userResult) {
                is AuthResult.Success -> userResult.data?.id
                is AuthResult.Error -> null
            }
            
            // Get nearby garage cars (within 15 miles)
            when (val result = garageCarRepository.getNearbyGarageCars(latitude, longitude, 15.0)) {
                is AuthResult.Success -> {
                    // Filter out current user's own cars
                    val nearbyCars = result.data.filter { 
                        it.garageCar.userId != currentUserId 
                    }
                    
                    if (nearbyCars.isEmpty()) {
                        loadingProgressBar.visibility = View.GONE
                        showEmptyState("No cars found within 15 miles. Check back later!")
                        return@launch
                    }

                    // Load car details for each garage car
                    when (val carsResult = carRepository.getAllCars()) {
                        is AuthResult.Success -> {
                            val carsMap = carsResult.data.associateBy { it.id }
                            
                            val displayItems = nearbyCars.mapNotNull { nearbyCar ->
                                val car = carsMap[nearbyCar.garageCar.carId]
                                if (car != null) {
                                    val imageUrl = CarImageService.getCarImageUrlSuspend(car.make)
                                    NearbyCarDisplayItem(
                                        id = nearbyCar.garageCar.id,
                                        carId = nearbyCar.garageCar.carId,
                                        make = car.make,
                                        model = car.model,
                                        year = nearbyCar.garageCar.year ?: 0,
                                        color = nearbyCar.garageCar.color,
                                        imageUrl = imageUrl,
                                        ownerName = nearbyCar.userProfile.displayName ?: nearbyCar.userProfile.username,
                                        distanceMiles = nearbyCar.distanceMiles
                                    )
                                } else {
                                    null
                                }
                            }
                            
                            loadingProgressBar.visibility = View.GONE
                            if (displayItems.isEmpty()) {
                                showEmptyState("No cars found nearby.")
                            } else {
                                popularBuildsRecyclerView.visibility = View.VISIBLE
                                nearbyCarAdapter.updateCars(displayItems)
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
                    showEmptyState("Failed to load nearby cars: ${result.message}")
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = View.VISIBLE
        popularBuildsRecyclerView.visibility = View.GONE
    }
}

// NearbyShopsActivity has been moved to its own file with full implementation




class ModTreeActivity : SimplePlaceholderActivity() {
    override val screenTitle: String = "Mod Tree"
    override val descriptionText: String =
        "Visualize the mod hierarchy for this car here."
}

class AddModActivity : SimplePlaceholderActivity() {
    override val screenTitle: String = "Add Mod"
    override val descriptionText: String =
        "Form to log completed mods with media will go here."
}

