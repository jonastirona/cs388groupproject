package com.example.modmycar

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddCarActivity : AppCompatActivity() {

    private var selectedMake: String? = null
    private var selectedModel: String? = null
    private var selectedColor: String = "#CCCCCC"

    // TODO: Replace with actual data from CarRepository
    private val placeholderMakes = listOf("Honda", "Toyota", "Ford", "Chevrolet", "BMW", "Mercedes-Benz")
    private val makeToModels = mapOf(
        "Honda" to listOf("Civic", "Accord", "CR-V", "Pilot"),
        "Toyota" to listOf("Camry", "Corolla", "RAV4", "Supra"),
        "Ford" to listOf("Mustang", "F-150", "Explorer", "Focus"),
        "Chevrolet" to listOf("Camaro", "Silverado", "Tahoe", "Corvette"),
        "BMW" to listOf("3 Series", "5 Series", "X5", "M3"),
        "Mercedes-Benz" to listOf("C-Class", "E-Class", "S-Class", "GLE")
    )

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

        // Setup Make dropdown
        val makeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, placeholderMakes)
        makeAutoComplete.setAdapter(makeAdapter)
        makeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedMake = placeholderMakes[position]
            selectedModel = null
            
            // Enable and populate model dropdown
            modelInputLayout.isEnabled = true
            modelAutoComplete.isEnabled = true
            val models = makeToModels[selectedMake] ?: emptyList()
            val modelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, models)
            modelAutoComplete.setAdapter(modelAdapter)
            modelAutoComplete.setText("", false)
        }

        // Setup Model dropdown
        modelAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val models = makeToModels[selectedMake] ?: emptyList()
            if (position < models.size) {
                selectedModel = models[position]
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
            
            // TODO: Implement save to Supabase
            Toast.makeText(this, "Car saved (placeholder): $selectedMake $selectedModel $year", Toast.LENGTH_SHORT).show()
            finish()
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
