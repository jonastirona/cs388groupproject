package com.example.modmycar

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Integration tests for Mod hierarchy and categories in Supabase.
 * Tests actual database operations for mods.
 * 
 * Must run on Android device/emulator (uses AndroidJUnit4)
 */
@RunWith(AndroidJUnit4::class)
class SupabaseModHierarchyIntegrationTests {

    private lateinit var modRepository: ModRepository
    private val createdModIds = mutableListOf<String>()

    @Before
    fun setup() {
        // Use regular SupabaseClient - we have Android runtime now
        modRepository = SupabaseModRepository()
    }

    @Test
    fun createModsWithCategoriesInSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: CREATE MODS WITH CATEGORIES ===")
        
        val mods = listOf(
            ModCreate(
                name = "Cold Air Intake",
                description = "PRL Motorsports CAI",
                category = "engine"
            ),
            ModCreate(
                name = "Cat-Back Exhaust",
                description = "AWE Touring Exhaust",
                category = "exhaust"
            ),
            ModCreate(
                name = "Coilover Suspension",
                description = "BC Racing Coilovers",
                category = "suspension"
            ),
            ModCreate(
                name = "Front Lip",
                description = "Carbon Fiber Front Lip",
                category = "exterior"
            )
        )

        println("Creating mods in Supabase with categories:")
        mods.forEach { mod ->
            println("  ${mod.category?.uppercase()}: ${mod.name}")
        }

        val createdMods = mutableListOf<Mod>()
        mods.forEach { modCreate ->
            val result = modRepository.createMod(modCreate)
            assertTrue("Mod creation should succeed", result is AuthResult.Success)
            
            if (result is AuthResult.Success) {
                val mod = result.data
                createdMods.add(mod)
                createdModIds.add(mod.id)
                println("    ✓ Created: ${mod.name} (ID: ${mod.id})")
            }
        }

        println("\n✓ All mods created successfully in Supabase!")
        println("  Total mods created: ${createdMods.size}")
        println("  Categories: ${createdMods.map { it.category }.distinct()}")
        
        // Verify categories
        assertEquals(4, createdMods.size)
        assertTrue(createdMods.any { it.category == "engine" })
        assertTrue(createdMods.any { it.category == "exhaust" })
        assertTrue(createdMods.any { it.category == "suspension" })
        assertTrue(createdMods.any { it.category == "exterior" })
        
        println("\n✓ Categories verified - check Supabase dashboard to see mods organized by category")
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun createParentAndChildModsInSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: MOD HIERARCHY ===")
        
        // Create parent mod
        println("1. Creating parent mod in Supabase...")
        val parentModCreate = ModCreate(
            name = "Turbo System",
            description = "Base turbo installation",
            parentModId = null, // Root mod
            category = "engine"
        )

        val parentResult = modRepository.createMod(parentModCreate)
        assertTrue("Parent mod creation should succeed", parentResult is AuthResult.Success)
        
        val parentMod = (parentResult as AuthResult.Success).data
        createdModIds.add(parentMod.id)
        println("   ✓ Parent mod created: ${parentMod.name} (ID: ${parentMod.id})")
        println("     Parent Mod ID: ${parentMod.parentModId ?: "None (root mod)"}")

        // Create child mods
        println("\n2. Creating child mods in Supabase...")
        val childModsCreate = listOf(
            ModCreate(
                name = "Intercooler Upgrade",
                description = "Upgraded intercooler",
                parentModId = parentMod.id, // References parent
                category = "engine"
            ),
            ModCreate(
                name = "Blow-Off Valve",
                description = "HKS BOV",
                parentModId = parentMod.id, // References same parent
                category = "engine"
            )
        )

        val childMods = mutableListOf<Mod>()
        childModsCreate.forEach { childCreate ->
            val result = modRepository.createMod(childCreate)
            assertTrue("Child mod creation should succeed", result is AuthResult.Success)
            
            if (result is AuthResult.Success) {
                val child = result.data
                childMods.add(child)
                createdModIds.add(child.id)
                println("   ✓ Child mod created: ${child.name} (ID: ${child.id})")
                println("     Parent Mod ID: ${child.parentModId}")
            }
        }

        println("\n✓ Hierarchy established in Supabase!")
        println("  Parent: ${parentMod.name}")
        println("  Children: ${childMods.map { it.name }}")

        // Verify hierarchy
        assertEquals(parentMod.id, childMods[0].parentModId)
        assertEquals(parentMod.id, childMods[1].parentModId)
        assertNull(parentMod.parentModId)

        // Read back from Supabase to verify
        println("\n3. Reading mods from Supabase to verify hierarchy...")
        val getChildrenResult = modRepository.getChildMods(parentMod.id)
        assertTrue("Get children should succeed", getChildrenResult is AuthResult.Success)
        
        val childrenFromDb = (getChildrenResult as AuthResult.Success).data
        println("   ✓ Retrieved ${childrenFromDb.size} child mods from Supabase")
        assertEquals(2, childrenFromDb.size)
        
        println("\n✓ Hierarchy verified in Supabase database!")
        println("  Check Supabase dashboard to see parent-child relationships")
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun getModsByCategoryFromSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: GET MODS BY CATEGORY ===")
        
        // Create mods in different categories
        val engineMod = modRepository.createMod(ModCreate(
            name = "ECU Tune",
            category = "engine"
        ))
        assertTrue(engineMod is AuthResult.Success)
        createdModIds.add((engineMod as AuthResult.Success).data.id)

        val exhaustMod = modRepository.createMod(ModCreate(
            name = "Downpipe",
            category = "exhaust"
        ))
        assertTrue(exhaustMod is AuthResult.Success)
        createdModIds.add((exhaustMod as AuthResult.Success).data.id)

        println("Created mods in different categories")
        println("Now querying Supabase for mods by category...\n")

        // Get mods by category
        val engineModsResult = modRepository.getModsByCategory("engine")
        assertTrue("Get engine mods should succeed", engineModsResult is AuthResult.Success)
        
        val engineMods = (engineModsResult as AuthResult.Success).data
        println("Engine mods from Supabase:")
        engineMods.forEach { mod ->
            println("  - ${mod.name} (ID: ${mod.id})")
        }

        val exhaustModsResult = modRepository.getModsByCategory("exhaust")
        assertTrue("Get exhaust mods should succeed", exhaustModsResult is AuthResult.Success)
        
        val exhaustMods = (exhaustModsResult as AuthResult.Success).data
        println("\nExhaust mods from Supabase:")
        exhaustMods.forEach { mod ->
            println("  - ${mod.name} (ID: ${mod.id})")
        }

        // Verify categories
        assertTrue(engineMods.any { it.category == "engine" })
        assertTrue(exhaustMods.any { it.category == "exhaust" })
        assertTrue(engineMods.all { it.category == "engine" })
        assertTrue(exhaustMods.all { it.category == "exhaust" })

        println("\n✓ Category filtering verified - mods properly organized in Supabase")
        println("=== TEST COMPLETE ===\n")
        }
    }

    @Test
    fun buildModTreeFromSupabase() {
        runBlocking {
        println("\n=== SUPABASE INTEGRATION TEST: BUILD MOD TREE ===")
        
        // Create a tree structure
        val root = modRepository.createMod(ModCreate(
            name = "Performance Package",
            parentModId = null,
            category = "engine"
        ))
        assertTrue(root is AuthResult.Success)
        val rootMod = (root as AuthResult.Success).data
        createdModIds.add(rootMod.id)

        val child1 = modRepository.createMod(ModCreate(
            name = "Turbo Kit",
            parentModId = rootMod.id,
            category = "engine"
        ))
        assertTrue(child1 is AuthResult.Success)
        val childMod1 = (child1 as AuthResult.Success).data
        createdModIds.add(childMod1.id)

        val child2 = modRepository.createMod(ModCreate(
            name = "Exhaust System",
            parentModId = rootMod.id,
            category = "exhaust"
        ))
        assertTrue(child2 is AuthResult.Success)
        val childMod2 = (child2 as AuthResult.Success).data
        createdModIds.add(childMod2.id)

        println("Created mod tree in Supabase:")
        println("  Root: ${rootMod.name}")
        println("    ├─ ${childMod1.name}")
        println("    └─ ${childMod2.name}")

        // Get mod tree from Supabase
        println("\nBuilding mod tree from Supabase database...")
        val treeResult = modRepository.getModTree()
        assertTrue("Get mod tree should succeed", treeResult is AuthResult.Success)
        
        val tree = (treeResult as AuthResult.Success).data
        println("\n✓ Mod tree retrieved from Supabase:")
        
        tree.forEach { rootNode ->
            println("  ${rootNode.mod.name}")
            rootNode.children.forEach { child ->
                println("    ├─ ${child.mod.name}")
            }
        }

        // Find our root in the tree
        val ourRoot = tree.find { it.mod.id == rootMod.id }
        assertNotNull("Root mod should be in tree", ourRoot)
        
        if (ourRoot != null) {
            println("\n✓ Tree structure verified:")
            println("  Root mod found: ${ourRoot.mod.name}")
            println("  Children count: ${ourRoot.children.size}")
        }

        println("\n✓ Mod tree successfully built from Supabase!")
        println("  Check Supabase dashboard to see the hierarchical structure")
        println("=== TEST COMPLETE ===\n")
        }
    }
}

