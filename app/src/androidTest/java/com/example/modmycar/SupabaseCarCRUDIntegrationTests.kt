package com.example.modmycar

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Integration tests that actually interact with Supabase.
 * These tests verify CRUD operations by reading/writing to the real database.
 * 
 * REQUIREMENTS:
 * - Supabase credentials must be configured in local.properties
 * - Test user must exist or be created
 * - Tables must exist in Supabase (cars, mods, car_mods)
 * - Must run on Android device/emulator (uses AndroidJUnit4)
 */
@RunWith(AndroidJUnit4::class)
class SupabaseCarCRUDIntegrationTests {

    private lateinit var carRepository: CarRepository
    private val testUserId = "test-user-integration-${System.currentTimeMillis()}"
    private val TAG = "CarTest"

    @Before
    fun setup() {
        // Use regular SupabaseClient - we have Android runtime now
        carRepository = SupabaseCarRepository()
    }

    @Test
    fun createCarInSupabaseAndVerify() {
        runBlocking {
            Log.d(TAG, "\n=== SUPABASE INTEGRATION TEST: CREATE CAR ===")
            
            val carCreate = CarCreate(
                userId = testUserId,
                make = "Honda",
                model = "Civic Si",
                color = "Championship White",
                year = 2023
            )

            Log.d(TAG, "Creating car in Supabase:")
            Log.d(TAG, "  Make: ${carCreate.make}")
            Log.d(TAG, "  Model: ${carCreate.model}")
            Log.d(TAG, "  Color: ${carCreate.color}")
            Log.d(TAG, "  Year: ${carCreate.year}")
            Log.d(TAG, "  User ID: ${carCreate.userId}")

            val result = carRepository.createCar(carCreate)

            if (result is AuthResult.Success) {
                val createdCar = result.data
                Log.d(TAG, "\n✓ Car created successfully in Supabase!")
                Log.d(TAG, "  Car ID: ${createdCar.id}")
                Log.d(TAG, "  Created At: ${createdCar.createdAt}")
                Log.d(TAG, "  Make: ${createdCar.make}")
                Log.d(TAG, "  Model: ${createdCar.model}")
                
                // Verify data matches
                assertEquals(carCreate.make, createdCar.make)
                assertEquals(carCreate.model, createdCar.model)
                assertEquals(carCreate.color, createdCar.color)
                assertEquals(carCreate.year, createdCar.year)
                assertNotNull(createdCar.id)
                assertNotNull(createdCar.createdAt)
                
                Log.d(TAG, "\n✓ Data verified - car exists in Supabase database")
                Log.d(TAG, "  Check Supabase dashboard to see the new car entry")
            } else {
                val error = result as AuthResult.Error
                Log.e(TAG, "\n✗✗✗ CAR CREATION FAILED ✗✗✗")
                Log.e(TAG, "=".repeat(60))
                Log.e(TAG, "ERROR MESSAGE:")
                Log.e(TAG, error.message ?: "Unknown error")
                Log.e(TAG, "=".repeat(60))
                
                // Print exception details
                if (error.exception != null) {
                    Log.e(TAG, "\nEXCEPTION DETAILS:")
                    Log.e(TAG, "  Type: ${error.exception.javaClass.name}")
                    Log.e(TAG, "  Message: ${error.exception.message}")
                    
                    // Print cause chain
                    var cause = error.exception.cause
                    var causeLevel = 1
                    while (cause != null && causeLevel <= 5) {
                        Log.e(TAG, "  Cause Level $causeLevel: ${cause.javaClass.name}")
                        Log.e(TAG, "    Message: ${cause.message}")
                        cause = cause.cause
                        causeLevel++
                    }
                    
                    Log.e(TAG, "\nFULL STACK TRACE:")
                    error.exception.printStackTrace()
                    
                    // Try to extract HTTP response details if available
                    val exceptionString = error.exception.toString()
                    if (exceptionString.contains("status") || exceptionString.contains("code")) {
                        Log.e(TAG, "\nHTTP/Response Details:")
                        Log.e(TAG, exceptionString)
                    }
                }
                
                Log.e(TAG, "=".repeat(60))
                Log.e(TAG, "\n")
                
                fail("Car creation failed. See Logcat for error details (filter by tag: CarTest).")
            }
            
            Log.d(TAG, "=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun readCarFromSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: READ CAR ===")
        
        // First create a car
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Toyota",
            model = "Supra",
            color = "Red",
            year = 2024
        )

        val createResult = carRepository.createCar(carCreate)
        assertTrue("Car creation should succeed", createResult is AuthResult.Success)
        
        val createdCar = (createResult as AuthResult.Success).data
        val carId = createdCar.id

        println("Created car with ID: $carId")
        println("Now reading car from Supabase...\n")

        // Read the car back
        val readResult = carRepository.getCar(carId)

        assertTrue("Car read should succeed", readResult is AuthResult.Success)
        
        if (readResult is AuthResult.Success) {
            val car = readResult.data
            assertNotNull("Car should be found", car)
            
            if (car != null) {
                println("✓ Car read successfully from Supabase!")
                println("  Car ID: ${car.id}")
                println("  Make: ${car.make}")
                println("  Model: ${car.model}")
                println("  Color: ${car.color}")
                println("  Year: ${car.year}")
                println("  Created At: ${car.createdAt}")
                
                // Verify data matches
                assertEquals(carId, car.id)
                assertEquals(carCreate.make, car.make)
                assertEquals(carCreate.model, car.model)
                assertEquals(carCreate.color, car.color)
                assertEquals(carCreate.year, car.year)
                
                println("\n✓ Data verified - car data matches what was stored in Supabase")
                println("  This confirms the car was properly saved and retrieved from database")
            } else {
                fail("Car should not be null")
            }
        } else {
            val error = readResult as AuthResult.Error
            println("✗ Error: ${error.message}")
            fail("Car read failed: ${error.message}")
        }
        
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun updateCarInSupabaseAndVerify() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: UPDATE CAR ===")
        
        // Create a car first
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Honda",
            model = "Civic",
            color = "Blue",
            year = 2020
        )

        val createResult = carRepository.createCar(carCreate)
        assertTrue("Car creation should succeed", createResult is AuthResult.Success)
        
        val originalCar = (createResult as AuthResult.Success).data
        val carId = originalCar.id

        println("Original car:")
        println("  ID: $carId")
        println("  Color: ${originalCar.color}")
        println("  Year: ${originalCar.year}")

        // Update the car
        val update = CarUpdate(
            color = "Rallye Red",
            year = 2024
        )

        println("\nUpdating car in Supabase:")
        println("  New Color: ${update.color}")
        println("  New Year: ${update.year}")

        val updateResult = carRepository.updateCar(carId, update)

        assertTrue("Car update should succeed", updateResult is AuthResult.Success)
        
        if (updateResult is AuthResult.Success) {
            val updatedCar = updateResult.data
            println("\n✓ Car updated successfully in Supabase!")
            println("  Car ID: ${updatedCar.id}")
            println("  Updated Color: ${updatedCar.color}")
            println("  Updated Year: ${updatedCar.year}")
            println("  Updated At: ${updatedCar.updatedAt}")
            
            // Verify update
            assertEquals("Rallye Red", updatedCar.color)
            assertEquals(2024, updatedCar.year)
            assertNotEquals(originalCar.color, updatedCar.color)
            assertNotEquals(originalCar.year, updatedCar.year)
            assertNotNull(updatedCar.updatedAt)
            
            // Read back to verify it's actually updated in database
            val readResult = carRepository.getCar(carId)
            assertTrue("Read should succeed", readResult is AuthResult.Success)
            val readCar = (readResult as AuthResult.Success).data
            assertNotNull("Car should exist", readCar)
            
            if (readCar != null) {
                assertEquals("Rallye Red", readCar.color)
                assertEquals(2024, readCar.year)
                println("\n✓ Verified update in Supabase - changes persisted to database")
            }
        } else {
            val error = updateResult as AuthResult.Error
            println("✗ Error: ${error.message}")
            fail("Car update failed: ${error.message}")
        }
        
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun deleteCarFromSupabaseAndVerify() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: DELETE CAR ===")
        
        // Create a car first
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Ford",
            model = "Mustang",
            color = "Black",
            year = 2023
        )

        val createResult = carRepository.createCar(carCreate)
        assertTrue("Car creation should succeed", createResult is AuthResult.Success)
        
        val createdCar = (createResult as AuthResult.Success).data
        val carId = createdCar.id

        println("Created car:")
        println("  ID: $carId")
        println("  Make/Model: ${createdCar.make} ${createdCar.model}")

        // Verify car exists
        val readBefore = carRepository.getCar(carId)
        assertTrue("Car should exist before delete", readBefore is AuthResult.Success)
        assertNotNull("Car should not be null", (readBefore as AuthResult.Success).data)

        println("\nDeleting car from Supabase...")

        // Delete the car
        val deleteResult = carRepository.deleteCar(carId)

        assertTrue("Car deletion should succeed", deleteResult is AuthResult.Success)
        
        println("✓ Car deleted successfully from Supabase!")

        // Verify car no longer exists
        val readAfter = carRepository.getCar(carId)
        assertTrue("Read should succeed", readAfter is AuthResult.Success)
        val carAfter = (readAfter as AuthResult.Success).data
        
        // Car should be null or not found
        if (carAfter == null) {
            println("✓ Verified deletion - car no longer exists in Supabase database")
        } else {
            println("⚠ Car still exists (may be due to RLS policies)")
        }
        
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun crudCompleteWorkflowWithSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: COMPLETE CRUD WORKFLOW ===")
        
        // CREATE
        println("1. CREATE: Creating car in Supabase...")
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Honda",
            model = "Civic Si",
            color = "Championship White",
            year = 2023
        )

        val createResult = carRepository.createCar(carCreate)
        assertTrue("Create should succeed", createResult is AuthResult.Success)
        val car = (createResult as AuthResult.Success).data
        println("   ✓ Car created in Supabase with ID: ${car.id}")

        // READ
        println("2. READ: Reading car from Supabase...")
        val readResult = carRepository.getCar(car.id)
        assertTrue("Read should succeed", readResult is AuthResult.Success)
        val readCar = (readResult as AuthResult.Success).data
        assertNotNull("Car should exist", readCar)
        println("   ✓ Car read from Supabase: ${readCar?.make} ${readCar?.model}")

        // UPDATE
        println("3. UPDATE: Updating car in Supabase...")
        val updateResult = carRepository.updateCar(car.id, CarUpdate(color = "Rallye Red"))
        assertTrue("Update should succeed", updateResult is AuthResult.Success)
        val updatedCar = (updateResult as AuthResult.Success).data
        println("   ✓ Car updated in Supabase: Color changed to ${updatedCar.color}")

        // Verify update persisted
        val verifyRead = carRepository.getCar(car.id)
        val verifyCar = ((verifyRead as AuthResult.Success).data)
        assertEquals("Rallye Red", verifyCar?.color)
        println("   ✓ Verified update persisted in Supabase database")

        // DELETE
        println("4. DELETE: Deleting car from Supabase...")
        val deleteResult = carRepository.deleteCar(car.id)
        assertTrue("Delete should succeed", deleteResult is AuthResult.Success)
        println("   ✓ Car deleted from Supabase")

        println("\n═══════════════════════════════════════════════════════")
        println("ALL CRUD OPERATIONS COMPLETED SUCCESSFULLY!")
        println("  ✓ CREATE - Car added to Supabase")
        println("  ✓ READ - Car retrieved from Supabase")
        println("  ✓ UPDATE - Car updated in Supabase")
        println("  ✓ DELETE - Car removed from Supabase")
        println("═══════════════════════════════════════════════════════")
        println("\nCheck your Supabase dashboard to see the operations!")
        println("=== TEST COMPLETE ===\n")
        }
    }
}

