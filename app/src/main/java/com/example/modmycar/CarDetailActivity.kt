package com.example.modmycar

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View

class CarDetailActivity : AppCompatActivity() {

    private var garageCarId: String? = null
    private var carId: String? = null
    private var currentColor: String = "#FF0000"
    private var currentYear: Int = 0
    private lateinit var modHierarchyRecyclerView: RecyclerView
    private lateinit var modHierarchyAdapter: ModHierarchyAdapter
    private lateinit var carMediaViewPager: ViewPager2
    private lateinit var carMediaAdapter: CarMediaAdapter

    private val modRepository: ModRepository = SupabaseModRepository()
    private val garageModRepository: GarageModRepository = SupabaseGarageModRepository()
    private val authRepository: AuthRepository = SupabaseAuthRepository()

    private var currentUserId: String? = null
    private var carOwnerId: String? = null
    private var isOwner: Boolean = false

    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.carDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Get car data from intent
        garageCarId = intent.getStringExtra("GARAGE_CAR_ID")
        carId = intent.getStringExtra("CAR_ID")
        val carMake = intent.getStringExtra("CAR_MAKE") ?: ""
        val carModel = intent.getStringExtra("CAR_MODEL") ?: ""
        currentYear = intent.getIntExtra("CAR_YEAR", 0)
        currentColor = intent.getStringExtra("CAR_COLOR") ?: "#FF0000"

        // Set car photo header
        val carPhotoHeader = findViewById<ImageView>(R.id.carPhotoHeaderImageView)
        lifecycleScope.launch {
            val carImageUrl = CarImageService.getCarImageUrlSuspend(carMake)
            Log.d("CarDetailActivity", "Car make: '$carMake', Image URL: $carImageUrl")
            if (carImageUrl != null) {
                Log.d("CarDetailActivity", "Loading image from URL: $carImageUrl")
                carPhotoHeader.load(carImageUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    // Enable memory and disk caching
                    memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    // Use a stable key based on make to ensure consistent caching
                    memoryCacheKey(carMake)
                    listener(
                        onStart = { request ->
                            Log.d("CarDetailActivity", "Image load started: ${request.data}")
                        },
                        onSuccess = { _, result ->
                            Log.d("CarDetailActivity", "Image loaded successfully for $carMake")
                        },
                        onError = { _, result ->
                            Log.e("CarDetailActivity", "Image load failed for $carMake: ${result.throwable.message}", result.throwable)
                        }
                    )
                }
            } else {
                Log.w("CarDetailActivity", "No image URL for make '$carMake' - using placeholder")
                carPhotoHeader.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Set car model and year
        val carModelYearTextView = findViewById<TextView>(R.id.carModelYearTextView)
        val yearShort = if (currentYear > 0) "'${currentYear.toString().takeLast(2)}" else ""
        carModelYearTextView.text = "$carMake $carModel $yearShort"

        // Set color preview (will be updated when garage car is loaded)
        updateColorPreview()

        // Color picker button
        findViewById<MaterialButton>(R.id.colorPickerButton).setOnClickListener {
            showColorPickerDialog()
        }

        // Setup mod hierarchy
        setupModHierarchy()

        // Setup car media carousel
        setupCarMedia()

        // Delete button
        findViewById<MaterialButton>(R.id.deleteCarButton).setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        // Load garage car to determine owner
        loadGarageCarOwner()

        // Add media button
        findViewById<FloatingActionButton>(R.id.addMediaFAB).setOnClickListener {
            Toast.makeText(this, "Add media placeholder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGarageCarOwner() {
        val localGarageCarId = garageCarId ?: return
        
        lifecycleScope.launch {
            when (val result = garageCarRepository.getGarageCar(localGarageCarId)) {
                is AuthResult.Success -> {
                    val garageCar = result.data
                    if (garageCar != null) {
                        carOwnerId = garageCar.userId
                        
                        // Update color and year from database
                        garageCar.color?.let { 
                            currentColor = it
                            updateColorPreview()
                        }
                        garageCar.year?.let { currentYear = it }
                        
                        // Get current user
                        val userResult = authRepository.getCurrentSession()
                        currentUserId = when (userResult) {
                            is AuthResult.Success -> userResult.data?.id
                            is AuthResult.Error -> null
                        }
                        
                        isOwner = currentUserId == carOwnerId
                        
                        // Hide edit buttons if not owner
                        if (!isOwner) {
                            findViewById<MaterialButton>(R.id.colorPickerButton).visibility = View.GONE
                            findViewById<MaterialButton>(R.id.deleteCarButton).visibility = View.GONE
                            findViewById<FloatingActionButton>(R.id.addMediaFAB).visibility = View.GONE
                        }
                        
                        // Update adapter to be read-only if not owner
                        modHierarchyAdapter = ModHierarchyAdapter(
                            emptyList(),
                            onModToggled = { modId, isCompleted ->
                                if (isOwner) {
                                    toggleGarageMod(modId, isCompleted)
                                }
                            },
                            isReadOnly = !isOwner
                        )
                        modHierarchyRecyclerView.adapter = modHierarchyAdapter
                        
                        // Load mod hierarchy for the car owner
                        val localCarId = carId
                        if (localCarId != null && carOwnerId != null) {
                            loadModHierarchy(localCarId, carOwnerId!!)
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@CarDetailActivity,
                        "Failed to load car: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun updateColorPreview() {
        val colorPreviewView = findViewById<android.view.View>(R.id.colorPreviewView)
        try {
            val color = Color.parseColor(currentColor)
            colorPreviewView.setBackgroundColor(color)
            // Also update the card background if needed
            val colorPreviewCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.colorPreviewCard)
            colorPreviewCard.setCardBackgroundColor(color)
        } catch (e: Exception) {
            val defaultColor = Color.parseColor("#FF0000")
            colorPreviewView.setBackgroundColor(defaultColor)
            val colorPreviewCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.colorPreviewCard)
            colorPreviewCard.setCardBackgroundColor(defaultColor)
        }
    }

    private fun setupModHierarchy() {
        modHierarchyRecyclerView = findViewById(R.id.modHierarchyRecyclerView)
        modHierarchyRecyclerView.layoutManager = LinearLayoutManager(this)

        // Adapter will be created/updated in loadGarageCarOwner() after we know if user is owner
        modHierarchyAdapter = ModHierarchyAdapter(
            emptyList(),
            onModToggled = { _, _ -> },
            isReadOnly = true // Default to read-only until we know ownership
        )
        modHierarchyRecyclerView.adapter = modHierarchyAdapter
    }

    private suspend fun loadModHierarchy(carId: String, ownerUserId: String) {
        // Get mod tree for this car
        val modTreeResult = modRepository.getModTree(carId)
        if (modTreeResult is AuthResult.Error) {
            Toast.makeText(
                this,
                "Failed to load mods: ${modTreeResult.message}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val modTree = (modTreeResult as AuthResult.Success).data

        // Get all mod IDs for this car
        val allModIdsForCar = modTree.flatMap { collectModIds(it) }.toSet()

        // Get completed mods for the car owner (not current user)
        val garageModsResult = garageModRepository.getGarageModsByUserId(ownerUserId)
        if (garageModsResult is AuthResult.Error) {
            Toast.makeText(
                this,
                "Failed to load completed mods: ${garageModsResult.message}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Filter to only mods that belong to this specific car
        val ownerMods = (garageModsResult as AuthResult.Success).data
        val completedModIds = ownerMods
            .filter { it.modId in allModIdsForCar } // Only mods for this car
            .map { it.modId }
            .toSet()

        fun mapTree(node: ModWithChildren): ModHierarchyItem {
            return ModHierarchyItem(
                modId = node.mod.id,
                name = node.mod.name,
                category = node.mod.category,
                isCompleted = completedModIds.contains(node.mod.id),
                children = node.children.map { child -> mapTree(child) }
            )
        }

        val rootItems = modTree.map { mapTree(it) }

        withContext(Dispatchers.Main) {
            modHierarchyAdapter.updateMods(rootItems)
        }
    }
    
    private fun collectModIds(node: ModWithChildren): List<String> {
        return listOf(node.mod.id) + node.children.flatMap { collectModIds(it) }
    }

    private fun toggleGarageMod(modId: String, isCompleted: Boolean) {
        if (!isOwner) {
            Toast.makeText(this, "You can only modify your own cars", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = currentUserId
        if (userId == null) {
            Toast.makeText(this, "Please sign in to track mods", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (isCompleted) {
                // Mark as completed by creating a garage_mod entry
                val create = GarageModCreate(
                    userId = userId,
                    modId = modId,
                    completedAt = null // Supabase can set this via triggers, or remain null
                )
                when (val result = garageModRepository.createGarageMod(create)) {
                    is AuthResult.Success -> {
                        // Reload hierarchy to reflect change
                        val localCarId = carId
                        if (localCarId != null && carOwnerId != null) {
                            loadModHierarchy(localCarId, carOwnerId!!)
                        }
                    }
                    is AuthResult.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Failed to mark mod complete: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                // Unmark: find existing garage_mod entries for this user+mod and delete them
                when (val gmResult = garageModRepository.getGarageModsByUserId(userId)) {
                    is AuthResult.Success -> {
                        val matching = gmResult.data.filter { it.modId == modId }
                        matching.forEach { gm ->
                            garageModRepository.deleteGarageMod(gm.id)
                        }

                        val localCarId = carId
                        if (localCarId != null && carOwnerId != null) {
                            loadModHierarchy(localCarId, carOwnerId!!)
                        }
                    }
                    is AuthResult.Error -> {
                        Toast.makeText(
                            this@CarDetailActivity,
                            "Failed to update mod: ${gmResult.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupCarMedia() {
        carMediaViewPager = findViewById(R.id.carMediaViewPager)
        
        // TODO: Replace with actual media URLs from storage
        val placeholderMedia = emptyList<String>()
        
        carMediaAdapter = CarMediaAdapter(placeholderMedia)
        carMediaViewPager.adapter = carMediaAdapter
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf("Red", "Blue", "Green", "Black", "White", "Silver", "Yellow", "Orange")
        val colorMap = mapOf(
            "Red" to "#FF0000",
            "Blue" to "#0000FF",
            "Green" to "#00FF00",
            "Black" to "#000000",
            "White" to "#FFFFFF",
            "Silver" to "#C0C0C0",
            "Yellow" to "#FFFF00",
            "Orange" to "#FFA500"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Select Color")
            .setItems(colors) { _, which ->
                val colorName = colors[which]
                val newColor = colorMap[colorName] ?: "#FF0000"
                updateCarColor(newColor)
            }
            .show()
    }
    
    private fun updateCarColor(newColor: String) {
        if (!isOwner) {
            Toast.makeText(this, "You can only modify your own cars", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (garageCarId == null) {
            Toast.makeText(this, "Car ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val update = GarageCarUpdate(color = newColor, year = currentYear)
            when (val result = garageCarRepository.updateGarageCar(garageCarId!!, update)) {
                is AuthResult.Success -> {
                    currentColor = newColor
                    updateColorPreview()
                    Toast.makeText(this@CarDetailActivity, "Color updated", Toast.LENGTH_SHORT).show()
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@CarDetailActivity,
                        "Failed to update color: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        if (garageCarId == null) {
            Toast.makeText(this, "Car ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delete Car")
            .setMessage("Are you sure you want to delete this car? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCar()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteCar() {
        if (!isOwner) {
            Toast.makeText(this, "You can only delete your own cars", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (garageCarId == null) {
            Toast.makeText(this, "Car ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            when (val result = garageCarRepository.deleteGarageCar(garageCarId!!)) {
                is AuthResult.Success -> {
                    Toast.makeText(this@CarDetailActivity, "Car deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@CarDetailActivity,
                        "Failed to delete car: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
