package com.example.modmycar

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddCarActivity : AppCompatActivity() {

    private var selectedMake: String? = null
    private var selectedModel: String? = null
    private var selectedColor: String = "#CCCCCC"
    
    private val carRepository: CarRepository = SupabaseCarRepository()
    private val garageCarRepository: GarageCarRepository = SupabaseGarageCarRepository()
    private val authRepository: AuthRepository = SupabaseAuthRepository()
    private val maintenanceRepository: MaintenanceRepository = SupabaseMaintenanceRepository()
    
    private var availableMakes: List<String> = emptyList()
    private var makeToModels: Map<String, List<String>> = emptyMap()
    private var currentModels: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        val toolbar = findViewById<MaterialToolbar>(R.id.addCarToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val makeInputLayout = findViewById<TextInputLayout>(R.id.makeInputLayout)
        val makeAutoComplete = findViewById<AutoCompleteTextView>(R.id.makeAutoComplete)
        val modelInputLayout = findViewById<TextInputLayout>(R.id.modelInputLayout)
        val modelAutoComplete = findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
        val yearEditText = findViewById<TextInputEditText>(R.id.yearEditText)
        val colorPickerButton = findViewById<MaterialButton>(R.id.colorPickerButton)
        val colorPreviewView = findViewById<android.view.View>(R.id.colorPreviewView)
        val colorHexTextView = findViewById<android.widget.TextView>(R.id.colorHexTextView)
        val saveCarButton = findViewById<MaterialButton>(R.id.saveCarButton)

        // Configure AutoCompleteTextView to show dropdown on click
        makeAutoComplete.threshold = 1
        makeAutoComplete.setOnClickListener {
            if (makeAutoComplete.adapter != null && makeAutoComplete.adapter.count > 0) {
                makeAutoComplete.showDropDown()
            }
        }
        
        modelAutoComplete.threshold = 1
        modelAutoComplete.setOnClickListener {
            if (modelAutoComplete.adapter != null && modelAutoComplete.adapter.count > 0) {
                modelAutoComplete.showDropDown()
            }
        }

        // Load makes and models from repository
        loadMakesAndModels()

        // Setup Make dropdown
        makeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < availableMakes.size) {
                selectedMake = availableMakes[position]
                selectedModel = null
                
                // Enable and populate model dropdown
                modelInputLayout.isEnabled = true
                modelAutoComplete.isEnabled = true
                loadModelsForMake(selectedMake!!)
            }
        }

        // Setup Model dropdown
        modelAutoComplete.setOnItemClickListener { _, _, position, _ ->
            if (position < currentModels.size) {
                selectedModel = currentModels[position]
            }
        }

        // Setup Color Picker
        colorPickerButton.setOnClickListener {
            showColorPickerDialog(colorPreviewView, colorHexTextView)
        }

        // Save Car Button
        saveCarButton.setOnClickListener {
            val yearText = yearEditText.text?.toString() ?: ""
            
            if (selectedMake == null) {
                Toast.makeText(this, "Please select a make", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedModel == null) {
                Toast.makeText(this, "Please select a model", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (yearText.isEmpty()) {
                Toast.makeText(this, "Please enter a year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val year = yearText.toIntOrNull()
            if (year == null || year < 1900 || year > 2100) {
                Toast.makeText(this, "Please enter a valid year", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveCar(selectedMake!!, selectedModel!!, year)
        }
    }

    private fun loadMakesAndModels() {
        lifecycleScope.launch {
            when (val result = carRepository.getMakesWithModels()) {
                is AuthResult.Success -> {
                    makeToModels = result.data
                    availableMakes = result.data.keys.sorted()
                    
                    if (availableMakes.isEmpty()) {
                        Toast.makeText(
                            this@AddCarActivity,
                            "No makes found in database",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    
                    val makeAdapter = ArrayAdapter(
                        this@AddCarActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        availableMakes
                    )
                    val makeAutoComplete = findViewById<AutoCompleteTextView>(R.id.makeAutoComplete)
                    makeAutoComplete.setAdapter(makeAdapter)
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@AddCarActivity,
                        "Failed to load makes: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun loadModelsForMake(make: String) {
        lifecycleScope.launch {
            when (val result = carRepository.getModelsByMake(make)) {
                is AuthResult.Success -> {
                    currentModels = result.data
                    
                    if (currentModels.isEmpty()) {
                        Toast.makeText(
                            this@AddCarActivity,
                            "No models found for $make",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    
                    val modelAdapter = ArrayAdapter(
                        this@AddCarActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        currentModels
                    )
                    val modelAutoComplete = findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
                    modelAutoComplete.setAdapter(modelAdapter)
                    modelAutoComplete.setText("", false)
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@AddCarActivity,
                        "Failed to load models: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun saveCar(make: String, model: String, year: Int) {
        lifecycleScope.launch {
            // Get current user
            val userResult = authRepository.getCurrentSession()
            val userId = when (userResult) {
                is AuthResult.Success -> userResult.data?.id
                is AuthResult.Error -> null
            }
            
            if (userId == null) {
                Toast.makeText(this@AddCarActivity, "Please sign in to save a car", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Find car_id by matching make and model
            val carsResult = carRepository.getAllCars()
            val carId = when (carsResult) {
                is AuthResult.Success -> {
                    carsResult.data.firstOrNull { it.make == make && it.model == model }?.id
                }
                is AuthResult.Error -> null
            }
            
            if (carId == null) {
                Toast.makeText(
                    this@AddCarActivity,
                    "Car not found: $make $model",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            
            // Create garage car
            val garageCarCreate = GarageCarCreate(
                userId = userId,
                carId = carId,
                color = selectedColor,
                year = year
            )
            
            when (val result = garageCarRepository.createGarageCar(garageCarCreate)) {
                is AuthResult.Success -> {
                    val newGarageCarId = result.data.id
                    
                    // Create default maintenance items for the new car
                    when (val maintenanceResult = maintenanceRepository.createDefaultMaintenanceItems(newGarageCarId)) {
                        is AuthResult.Success -> {
                            // Clear image cache to ensure fresh images are loaded when returning to garage
                            CarImageService.clearCache()
                            Toast.makeText(
                                this@AddCarActivity,
                                "Car saved successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        is AuthResult.Error -> {
                            // Car was created but maintenance items failed - still finish but warn user
                            CarImageService.clearCache()
                            Toast.makeText(
                                this@AddCarActivity,
                                "Car saved, but maintenance setup failed: ${maintenanceResult.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@AddCarActivity,
                        "Failed to save car: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showColorPickerDialog(colorPreviewView: android.view.View, colorHexTextView: android.widget.TextView) {
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
                selectedColor = colorMap[colorName] ?: "#CCCCCC"
                colorPreviewView.setBackgroundColor(Color.parseColor(selectedColor))
                colorHexTextView.text = selectedColor
            }
            .show()
    }
}
