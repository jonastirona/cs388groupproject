package com.example.modmycar

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CarDetailActivity : AppCompatActivity() {

    private var currentColor: String = "#FF0000"
    private lateinit var modHierarchyRecyclerView: RecyclerView
    private lateinit var modHierarchyAdapter: ModHierarchyAdapter
    private lateinit var carMediaViewPager: ViewPager2
    private lateinit var carMediaAdapter: CarMediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.carDetailToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Get car data from intent
        val carMake = intent.getStringExtra("CAR_MAKE") ?: ""
        val carModel = intent.getStringExtra("CAR_MODEL") ?: ""
        val carYear = intent.getIntExtra("CAR_YEAR", 0)
        currentColor = intent.getStringExtra("CAR_COLOR") ?: "#FF0000"

        // Set car photo header (placeholder)
        val carPhotoHeader = findViewById<ImageView>(R.id.carPhotoHeaderImageView)
        carPhotoHeader.setImageResource(android.R.drawable.ic_menu_gallery)

        // Set car model and year
        val carModelYearTextView = findViewById<TextView>(R.id.carModelYearTextView)
        val yearShort = if (carYear > 0) "'${carYear.toString().takeLast(2)}" else ""
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
        // Simple color picker dialog (placeholder)
        // In production, use a proper color picker library
        val colors = arrayOf("Red", "Blue", "Green", "Black", "White", "Silver")
        AlertDialog.Builder(this)
            .setTitle("Select Color")
            .setItems(colors) { _, which ->
                val colorMap = mapOf(
                    0 to "#FF0000",
                    1 to "#0000FF",
                    2 to "#00FF00",
                    3 to "#000000",
                    4 to "#FFFFFF",
                    5 to "#C0C0C0"
                )
                currentColor = colorMap[which] ?: "#FF0000"
                findViewById<android.view.View>(R.id.colorPreviewView)
                    .setBackgroundColor(Color.parseColor(currentColor))
                Toast.makeText(this, "Color changed (placeholder)", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Car")
            .setMessage("Are you sure you want to delete this car? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: Implement delete functionality
                Toast.makeText(this, "Car deleted (placeholder)", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
