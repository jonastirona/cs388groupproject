package com.example.modmycar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class NearbyShopsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesRepository: PlacesRepository
    private var userLocation: LatLng? = null
    private val shops = mutableListOf<Shop>()
    private val markers = mutableMapOf<String, Marker>()
    private var selectedShop: Shop? = null
    private var isFetchingDetails = false
    private var currentlyDisplayedPlaceId: String? = null

    private lateinit var shopDetailsCard: MaterialCardView
    private lateinit var shopName: TextView
    private lateinit var shopAddress: TextView
    private lateinit var shopType: TextView
    private lateinit var shopDistance: TextView
    private lateinit var shopRating: TextView
    private lateinit var shopRatingStars: TextView
    private lateinit var shopRatingContainer: LinearLayout
    private lateinit var shopHours: TextView
    private lateinit var shopPhoneButton: MaterialButton
    private lateinit var shopWebsiteButton: MaterialButton
    private lateinit var closeButton: ImageButton
    private lateinit var loadingCard: MaterialCardView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var shopsCountCard: MaterialCardView
    private lateinit var shopsCountText: TextView

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Location permission is required to find nearby shops",
                    Toast.LENGTH_LONG
                ).show()
                // Use a default location (e.g., a major city center) if permission denied
                userLocation = LatLng(37.7749, -122.4194) // San Francisco default
                initializeMap()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_shops)

        setupToolbar()
        setupViews()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        placesRepository = GooglePlacesRepository(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViews() {
        shopDetailsCard = findViewById(R.id.shopDetailsCard)
        shopName = findViewById(R.id.shopName)
        shopAddress = findViewById(R.id.shopAddress)
        shopType = findViewById(R.id.shopType)
        shopDistance = findViewById(R.id.shopDistance)
        shopRating = findViewById(R.id.shopRating)
        shopRatingStars = findViewById(R.id.shopRatingStars)
        shopRatingContainer = findViewById(R.id.shopRatingContainer)
        shopHours = findViewById(R.id.shopHours)
        shopPhoneButton = findViewById(R.id.shopPhoneButton)
        shopWebsiteButton = findViewById(R.id.shopWebsiteButton)
        closeButton = findViewById(R.id.closeButton)
        loadingCard = findViewById(R.id.loadingCard)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        loadingText = findViewById(R.id.loadingText)
        shopsCountCard = findViewById(R.id.shopsCountCard)
        shopsCountText = findViewById(R.id.shopsCountText)

        shopPhoneButton.setOnClickListener {
            selectedShop?.phone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

        shopWebsiteButton.setOnClickListener {
            selectedShop?.website?.let { website ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                startActivity(intent)
            }
        }

        closeButton.setOnClickListener {
            shopDetailsCard.visibility = View.GONE
            currentlyDisplayedPlaceId = null
            selectedShop = null
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
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

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                initializeMap()
            } else {
                // Use default location if location is null
                userLocation = LatLng(37.7749, -122.4194)
                initializeMap()
            }
        }.addOnFailureListener {
            // Use default location on failure
            userLocation = LatLng(37.7749, -122.4194)
            initializeMap()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        
        // Enable map controls
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        
        if (userLocation != null) {
            initializeMap()
        }
    }

    private fun initializeMap() {
        val location = userLocation ?: return
        
        // Enable user location on map
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }

        // Move camera to user location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13f))

        // Search for nearby shops
        searchNearbyShops(location.latitude, location.longitude)
    }

    private fun searchNearbyShops(latitude: Double, longitude: Double) {
        loadingCard.visibility = View.VISIBLE
        shopsCountCard.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = placesRepository.searchNearbyShops(latitude, longitude, 5000)
            
            when (result) {
                is AuthResult.Success -> {
                    shops.clear()
                    shops.addAll(result.data)
                    addShopsToMap()
                    updateShopsCount()
                    loadingCard.visibility = View.GONE
                    if (shops.isNotEmpty()) {
                        shopsCountCard.visibility = View.VISIBLE
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@NearbyShopsActivity,
                        "Failed to load shops: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadingCard.visibility = View.GONE
                }
            }
        }
    }

    private fun updateShopsCount() {
        val count = shops.size
        shopsCountText.text = when {
            count == 0 -> "No shops found"
            count == 1 -> "1 shop found"
            else -> "$count shops found"
        }
    }

    private fun addShopsToMap() {
        map.clear()
        markers.clear()
        
        shops.forEach { shop ->
            val position = LatLng(shop.latitude, shop.longitude)
            val snippet = buildString {
                append(shop.address)
                shop.rating?.let { rating ->
                    append("\n⭐ $rating/5.0")
                }
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(shop.name)
                    .snippet(snippet)
            )
            marker?.let {
                markers[shop.id] = it
                it.tag = shop
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val shop = marker.tag as? Shop ?: return false
        
        // Prevent switching if clicking the same shop
        if (selectedShop?.placeId == shop.placeId && shopDetailsCard.visibility == View.VISIBLE) {
            return true
        }
        
        selectedShop = shop
        showShopDetails(shop)
        return true
    }

    private fun showShopDetails(shop: Shop) {
        // Prevent re-displaying the same shop
        if (currentlyDisplayedPlaceId == shop.placeId && shopDetailsCard.visibility == View.VISIBLE) {
            return
        }
        
        currentlyDisplayedPlaceId = shop.placeId
        selectedShop = shop
        
        shopName.text = shop.name
        shopAddress.text = shop.address

        // Display shop type
        val shopTypeText = getShopTypeLabel(shop.types)
        if (shopTypeText.isNotEmpty()) {
            shopType.text = shopTypeText
            shopType.visibility = View.VISIBLE
        } else {
            shopType.visibility = View.GONE
        }

        // Calculate and display distance
        userLocation?.let { userLoc ->
            val distance = calculateDistance(
                userLoc.latitude,
                userLoc.longitude,
                shop.latitude,
                shop.longitude
            )
            shopDistance.text = formatDistance(distance)
            shopDistance.visibility = View.VISIBLE
        } ?: run {
            shopDistance.visibility = View.GONE
        }

        // Display rating with stars
        if (shop.rating != null) {
            shopRating.text = String.format("%.1f", shop.rating)
            shopRatingStars.text = getStarRating(shop.rating)
            shopRatingContainer.visibility = View.VISIBLE
        } else {
            shopRatingContainer.visibility = View.GONE
        }

        // Display opening hours
        if (!shop.openingHours.isNullOrEmpty()) {
            val currentDayHours = getCurrentDayHours(shop.openingHours)
            if (currentDayHours.isNotEmpty()) {
                shopHours.text = "Hours: $currentDayHours"
                shopHours.visibility = View.VISIBLE
            } else {
                shopHours.visibility = View.GONE
            }
        } else {
            shopHours.visibility = View.GONE
        }

        shopPhoneButton.visibility = if (shop.phone != null) View.VISIBLE else View.GONE
        shopWebsiteButton.visibility = if (shop.website != null) View.VISIBLE else View.GONE

        shopDetailsCard.visibility = View.VISIBLE

        // Fetch detailed information if we only have basic info (and not already fetching)
        val needsDetails = shop.phone == null || shop.website == null || shop.openingHours.isNullOrEmpty()
        if (needsDetails && !isFetchingDetails) {
            fetchShopDetails(shop.placeId)
        }
    }

    private fun getShopTypeLabel(types: List<String>?): String {
        if (types.isNullOrEmpty()) return ""
        
        val typeMap = mapOf(
            "car_repair" to "Auto Repair",
            "car_parts_store" to "Parts Store",
            "automotive_repair_shop" to "Auto Shop",
            "car_dealer" to "Car Dealer",
            "gas_station" to "Gas Station"
        )
        
        return types.firstOrNull { typeMap.containsKey(it) }?.let { typeMap[it] } ?: 
               types.firstOrNull()?.replace("_", " ")?.split(" ")?.joinToString(" ") { 
                   it.replaceFirstChar { char -> char.uppercaseChar() }
               } ?: ""
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble() // Distance in meters
    }

    private fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.roundToInt()} m away"
            else -> String.format("%.1f mi away", distanceMeters / 1609.34)
        }
    }

    private fun getStarRating(rating: Double): String {
        val fullStars = rating.toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5
        val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0
        
        return "★".repeat(fullStars) + 
               (if (hasHalfStar) "½" else "") + 
               "☆".repeat(emptyStars)
    }

    private fun getCurrentDayHours(hours: List<String>): String {
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val dayIndex = (dayOfWeek + 5) % 7 // Convert to Monday=0 format
        
        return hours.getOrNull(dayIndex) ?: ""
    }

    private fun fetchShopDetails(placeId: String) {
        // Prevent duplicate fetches
        if (isFetchingDetails || currentlyDisplayedPlaceId != placeId) {
            return
        }
        
        isFetchingDetails = true
        
        lifecycleScope.launch {
            try {
                val result = placesRepository.getPlaceDetails(placeId)
                
                when (result) {
                    is AuthResult.Success -> {
                        result.data?.let { detailedShop ->
                            // Update the shop in the list
                            val index = shops.indexOfFirst { it.id == detailedShop.id }
                            if (index >= 0) {
                                shops[index] = detailedShop
                                
                                // Update marker tag
                                markers[detailedShop.id]?.tag = detailedShop
                                
                                // Only update display if this is still the selected shop
                                if (selectedShop?.placeId == placeId && currentlyDisplayedPlaceId == placeId) {
                                    selectedShop = detailedShop
                                    showShopDetails(detailedShop)
                                }
                            }
                        }
                    }
                    is AuthResult.Error -> {
                        // Silently fail - we already have basic info displayed
                    }
                }
            } finally {
                isFetchingDetails = false
            }
        }
    }
}

