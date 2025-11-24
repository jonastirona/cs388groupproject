package com.example.modmycar

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CarDetailActivity : AppCompatActivity() {

    private var garageCarId: String? = null
    private var currentColor: String = "#FF0000"
    private var currentYear: Int = 0
    private lateinit var modHierarchyRecyclerView: RecyclerView
    private lateinit var modHierarchyAdapter: ModHierarchyAdapter
    private lateinit var carMediaViewPager: ViewPager2
    private lateinit var carMediaAdapter: CarMediaAdapter
    
    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.carDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Get car data from intent
        garageCarId = intent.getStringExtra("GARAGE_CAR_ID")
        val carMake = intent.getStringExtra("CAR_MAKE") ?: ""
        val carModel = intent.getStringExtra("CAR_MODEL") ?: ""
        currentYear = intent.getIntExtra("CAR_YEAR", 0)
        currentColor = intent.getStringExtra("CAR_COLOR") ?: "#FF0000"

        // Set car photo header (placeholder)
        val carPhotoHeader = findViewById<ImageView>(R.id.carPhotoHeaderImageView)
        carPhotoHeader.setImageResource(android.R.drawable.ic_menu_gallery)

        // Set car model and year
        val carModelYearTextView = findViewById<TextView>(R.id.carModelYearTextView)
        val yearShort = if (currentYear > 0) "'${currentYear.toString().takeLast(2)}" else ""
        carModelYearTextView.text = "$carMake $carModel $yearShort"

        // Set color preview
        val colorPreviewView = findViewById<android.view.View>(R.id.colorPreviewView)
        try {
            colorPreviewView.setBackgroundColor(Color.parseColor(currentColor))
        } catch (e: Exception) {
            colorPreviewView.setBackgroundColor(Color.parseColor("#FF0000"))
        }

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

        // Add media button
        findViewById<FloatingActionButton>(R.id.addMediaFAB).setOnClickListener {
            Toast.makeText(this, "Add media placeholder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupModHierarchy() {
        modHierarchyRecyclerView = findViewById(R.id.modHierarchyRecyclerView)
        modHierarchyRecyclerView.layoutManager = LinearLayoutManager(this)

        // TODO: Replace with actual mod data from repository
        val placeholderMods = listOf(
            ModHierarchyItem(
                modId = "1",
                name = "Engine",
                category = "engine",
                children = listOf(
                    ModHierarchyItem("1-1", "Forged Rods", "engine"),
                    ModHierarchyItem("1-2", "Forged Pistons", "engine"),
                    ModHierarchyItem("1-3", "Crankshaft", "engine")
                )
            ),
            ModHierarchyItem(
                modId = "2",
                name = "Exhaust",
                category = "exhaust",
                children = listOf(
                    ModHierarchyItem("2-1", "Manifold", "exhaust"),
                    ModHierarchyItem("2-2", "Catalytic Converter", "exhaust"),
                    ModHierarchyItem("2-3", "Resonator", "exhaust"),
                    ModHierarchyItem("2-4", "Muffler", "exhaust")
                )
            ),
            ModHierarchyItem(
                modId = "3",
                name = "Forced Induction",
                category = "forced_induction",
                children = listOf(
                    ModHierarchyItem("3-1", "Supercharger", "forced_induction"),
                    ModHierarchyItem("3-2", "Turbo", "forced_induction")
                )
            ),
            ModHierarchyItem(
                modId = "4",
                name = "Transmission",
                category = "transmission",
                children = listOf(
                    ModHierarchyItem("4-1", "Clutch", "transmission")
                )
            )
        )

        modHierarchyAdapter = ModHierarchyAdapter(placeholderMods)
        modHierarchyRecyclerView.adapter = modHierarchyAdapter
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
        if (garageCarId == null) {
            Toast.makeText(this, "Car ID not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val update = GarageCarUpdate(color = newColor, year = currentYear)
            when (val result = garageCarRepository.updateGarageCar(garageCarId!!, update)) {
                is AuthResult.Success -> {
                    currentColor = newColor
                    findViewById<android.view.View>(R.id.colorPreviewView)
                        .setBackgroundColor(Color.parseColor(currentColor))
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
