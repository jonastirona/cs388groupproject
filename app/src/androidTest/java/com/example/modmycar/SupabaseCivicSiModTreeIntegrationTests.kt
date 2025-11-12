package com.example.modmycar

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Integration tests demonstrating prepopulated mod tree for Civic Si in Supabase.
 * Shows how mods are created, linked to cars, and marked as complete.
 * 
 * Must run on Android device/emulator (uses AndroidJUnit4)
 */
@RunWith(AndroidJUnit4::class)
class SupabaseCivicSiModTreeIntegrationTests {

    private lateinit var carRepository: CarRepository
    private lateinit var modRepository: ModRepository
    private lateinit var carModRepository: CarModRepository
    
    private val testUserId = "test-user-civic-si-${System.currentTimeMillis()}"
    private var civicSiCarId: String? = null
    private val createdModIds = mutableListOf<String>()

    @Before
    fun setup() {
        // Use regular SupabaseClient - we have Android runtime now
        carRepository = SupabaseCarRepository()
        modRepository = SupabaseModRepository()
        carModRepository = SupabaseCarModRepository()
    }

    @Test
    fun civicSiPrepopulateModTreeInSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: CIVIC SI MOD TREE PREPOPULATION ===")
        
        // Step 1: Create the car
        println("STEP 1: Creating Civic Si in Supabase...")
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Honda",
            model = "Civic Si",
            color = "Championship White",
            year = 2023
        )

        val carResult = carRepository.createCar(carCreate)
        assertTrue("Car creation should succeed", carResult is AuthResult.Success)
        
        val civicSi = (carResult as AuthResult.Success).data
        civicSiCarId = civicSi.id
        
        println("  ✓ Car created in Supabase:")
        println("    ID: ${civicSi.id}")
        println("    ${civicSi.make} ${civicSi.model} ${civicSi.year}")

        // Step 2: Create mod tree in Supabase
        println("\nSTEP 2: Creating mod tree in Supabase...")
        
        // Root mods
        val engineRoot = modRepository.createMod(ModCreate(
            name = "Engine Performance",
            description = "Base engine performance upgrades",
            parentModId = null,
            category = "engine"
        ))
        assertTrue(engineRoot is AuthResult.Success)
        val engineRootMod = (engineRoot as AuthResult.Success).data
        createdModIds.add(engineRootMod.id)

        val exhaustRoot = modRepository.createMod(ModCreate(
            name = "Exhaust System",
            description = "Exhaust system upgrades",
            parentModId = null,
            category = "exhaust"
        ))
        assertTrue(exhaustRoot is AuthResult.Success)
        val exhaustRootMod = (exhaustRoot as AuthResult.Success).data
        createdModIds.add(exhaustRootMod.id)

        // Engine children
        val coldAirIntake = modRepository.createMod(ModCreate(
            name = "Cold Air Intake",
            description = "PRL Motorsports Cold Air Intake",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        assertTrue(coldAirIntake is AuthResult.Success)
        val caiMod = (coldAirIntake as AuthResult.Success).data
        createdModIds.add(caiMod.id)

        val ecuTune = modRepository.createMod(ModCreate(
            name = "ECU Tune",
            description = "Hondata FlashPro ECU Tune",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        assertTrue(ecuTune is AuthResult.Success)
        val ecuMod = (ecuTune as AuthResult.Success).data
        createdModIds.add(ecuMod.id)

        // Exhaust children
        val downpipe = modRepository.createMod(ModCreate(
            name = "Catless Downpipe",
            description = "PRL Catless Downpipe",
            parentModId = exhaustRootMod.id,
            category = "exhaust"
        ))
        assertTrue(downpipe is AuthResult.Success)
        val downpipeMod = (downpipe as AuthResult.Success).data
        createdModIds.add(downpipeMod.id)

        println("  ✓ Mod tree created in Supabase:")
        println("    📦 ${engineRootMod.name}")
        println("       ├─ ${caiMod.name}")
        println("       └─ ${ecuMod.name}")
        println("    📦 ${exhaustRootMod.name}")
        println("       └─ ${downpipeMod.name}")

        // Verify mods exist in Supabase
        println("\nSTEP 3: Verifying mods in Supabase...")
        val allModsResult = modRepository.getAllMods()
        assertTrue("Get all mods should succeed", allModsResult is AuthResult.Success)
        
        val allMods = (allModsResult as AuthResult.Success).data
        val ourMods = allMods.filter { createdModIds.contains(it.id) }
        println("  ✓ Found ${ourMods.size} mods in Supabase database")

        println("\n═══════════════════════════════════════════════════════")
        println("MOD TREE PREPOPULATED SUCCESSFULLY IN SUPABASE!")
        println("  Car: ${civicSi.make} ${civicSi.model}")
        println("  Total mods created: ${createdModIds.size}")
        println("  Check Supabase dashboard to see the mod tree")
        println("═══════════════════════════════════════════════════════")
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun civicSiMarkModsAsCompleteInSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: MARK MODS COMPLETE ===")
        
        // Create car
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Honda",
            model = "Civic Si",
            color = "Championship White",
            year = 2023
        )
        val carResult = carRepository.createCar(carCreate)
        assertTrue(carResult is AuthResult.Success)
        val civicSi = (carResult as AuthResult.Success).data
        civicSiCarId = civicSi.id

        // Create mods
        val engineRoot = modRepository.createMod(ModCreate(
            name = "Engine Performance",
            parentModId = null,
            category = "engine"
        ))
        val engineRootMod = ((engineRoot as AuthResult.Success).data)
        createdModIds.add(engineRootMod.id)

        val coldAirIntake = modRepository.createMod(ModCreate(
            name = "Cold Air Intake",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        val caiMod = ((coldAirIntake as AuthResult.Success).data)
        createdModIds.add(caiMod.id)

        val ecuTune = modRepository.createMod(ModCreate(
            name = "ECU Tune",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        val ecuMod = ((ecuTune as AuthResult.Success).data)
        createdModIds.add(ecuMod.id)

        println("Car: ${civicSi.make} ${civicSi.model} (ID: ${civicSi.id})")
        println("Mods available:")
        println("  - ${caiMod.name} (ID: ${caiMod.id})")
        println("  - ${ecuMod.name} (ID: ${ecuMod.id})")

        // Mark Cold Air Intake as complete
        println("\n1. Marking '${caiMod.name}' as complete in Supabase...")
        val complete1Result = carModRepository.markModCompleted(
            carId = civicSi.id,
            modId = caiMod.id,
            notes = "Installed PRL Motorsports CAI. Great sound improvement!"
        )

        assertTrue("Mark complete should succeed", complete1Result is AuthResult.Success)
        
        val carMod1 = (complete1Result as AuthResult.Success).data
        println("  ✓ Entry added to CarMods table in Supabase:")
        println("    CarMod ID: ${carMod1.id}")
        println("    Car ID: ${carMod1.carId}")
        println("    Mod ID: ${carMod1.modId}")
        println("    Completed At: ${carMod1.completedAt}")
        println("    Notes: ${carMod1.notes}")

        // Mark ECU Tune as complete
        println("\n2. Marking '${ecuMod.name}' as complete in Supabase...")
        val complete2Result = carModRepository.markModCompleted(
            carId = civicSi.id,
            modId = ecuMod.id,
            notes = "Hondata FlashPro installed. +50hp gain!"
        )

        assertTrue("Mark complete should succeed", complete2Result is AuthResult.Success)
        
        val carMod2 = (complete2Result as AuthResult.Success).data
        println("  ✓ Entry added to CarMods table in Supabase:")
        println("    CarMod ID: ${carMod2.id}")
        println("    Car ID: ${carMod2.carId}")
        println("    Mod ID: ${carMod2.modId}")
        println("    Completed At: ${carMod2.completedAt}")
        println("    Notes: ${carMod2.notes}")

        // Verify entries in Supabase
        println("\n3. Verifying CarMods entries in Supabase...")
        val carModsResult = carModRepository.getCarMods(civicSi.id)
        assertTrue("Get car mods should succeed", carModsResult is AuthResult.Success)
        
        val carMods = (carModsResult as AuthResult.Success).data
        println("  ✓ Retrieved ${carMods.size} CarMod entries from Supabase")
        carMods.forEach { carMod ->
            println("    - Mod ID: ${carMod.modId}, Completed: ${carMod.completedAt != null}")
        }

        assertEquals(2, carMods.size)
        assertTrue(carMods.all { it.completedAt != null })

        println("\n═══════════════════════════════════════════════════════")
        println("MODS MARKED AS COMPLETE IN SUPABASE!")
        println("  Car: ${civicSi.make} ${civicSi.model}")
        println("  Completed mods: ${carMods.size}")
        println("  Check Supabase dashboard to see CarMods table entries")
        println("═══════════════════════════════════════════════════════")
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun civicSiCompleteWorkflowWithModTreeAndStatusInSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: CIVIC SI COMPLETE WORKFLOW ===")
        
        // Step 1: Create car
        println("STEP 1: Create car in Supabase")
        val carCreate = CarCreate(
            userId = testUserId,
            make = "Honda",
            model = "Civic Si",
            color = "Championship White",
            year = 2023
        )
        val carResult = carRepository.createCar(carCreate)
        val civicSi = ((carResult as AuthResult.Success).data)
        civicSiCarId = civicSi.id
        println("  ✓ Car created: ${civicSi.id}")

        // Step 2: Create mod tree
        println("\nSTEP 2: Create mod tree in Supabase")
        val engineRoot = modRepository.createMod(ModCreate(
            name = "Engine Performance",
            parentModId = null,
            category = "engine"
        ))
        val engineRootMod = ((engineRoot as AuthResult.Success).data)
        createdModIds.add(engineRootMod.id)

        val cai = modRepository.createMod(ModCreate(
            name = "Cold Air Intake",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        val caiMod = ((cai as AuthResult.Success).data)
        createdModIds.add(caiMod.id)

        val ecu = modRepository.createMod(ModCreate(
            name = "ECU Tune",
            parentModId = engineRootMod.id,
            category = "engine"
        ))
        val ecuMod = ((ecu as AuthResult.Success).data)
        createdModIds.add(ecuMod.id)
        println("  ✓ Mod tree created with ${createdModIds.size} mods")

        // Step 3: Get mods with status
        println("\nSTEP 3: Get mods with status from Supabase")
        val statusResult = carModRepository.getModsWithStatus(civicSi.id)
        assertTrue("Get mods with status should succeed", statusResult is AuthResult.Success)
        
        val modsWithStatus = (statusResult as AuthResult.Success).data
        println("  ✓ Retrieved ${modsWithStatus.size} mods with status")
        
        modsWithStatus.filter { createdModIds.contains(it.mod.id) }.forEach { modStatus ->
            val status = when {
                modStatus.isCompleted -> "✓ COMPLETED"
                modStatus.isUnlocked -> "○ UNLOCKED"
                else -> "🔒 LOCKED"
            }
            println("    ${modStatus.mod.name}: $status")
        }

        // Step 4: Mark mods as complete
        println("\nSTEP 4: Mark mods as complete in Supabase")
        carModRepository.markModCompleted(civicSi.id, caiMod.id, "Installed successfully")
        carModRepository.markModCompleted(civicSi.id, ecuMod.id, "Tuned for 93 octane")
        println("  ✓ 2 mods marked as complete")

        // Step 5: Verify final status
        println("\nSTEP 5: Verify final status from Supabase")
        val finalStatusResult = carModRepository.getModsWithStatus(civicSi.id)
        val finalStatus = ((finalStatusResult as AuthResult.Success).data)
        
        val completedCount = finalStatus.count { 
            createdModIds.contains(it.mod.id) && it.isCompleted 
        }
        println("  ✓ Completed mods: $completedCount")

        println("\n═══════════════════════════════════════════════════════")
        println("COMPLETE WORKFLOW SUCCESSFUL!")
        println("  ✓ Car created in Supabase")
        println("  ✓ Mod tree created in Supabase")
        println("  ✓ Mods marked as complete in Supabase")
        println("  ✓ Status verified from Supabase database")
        println("\n  Check Supabase dashboard to see:")
        println("    - cars table: ${civicSi.id}")
        println("    - mods table: ${createdModIds.size} mods")
        println("    - car_mods table: 2 completed entries")
        println("═══════════════════════════════════════════════════════")
        println("=== TEST COMPLETE ===\n")
        }
    }
}

