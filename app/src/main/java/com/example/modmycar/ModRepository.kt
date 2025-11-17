package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

interface ModRepository {
    suspend fun getMod(modId: String): AuthResult<Mod?>
    suspend fun getModsByCarId(carId: String): AuthResult<List<Mod>>
    suspend fun getRootMods(carId: String): AuthResult<List<Mod>>
    suspend fun getChildMods(parentId: String): AuthResult<List<Mod>>
    suspend fun getModTree(carId: String): AuthResult<List<ModWithChildren>>
}

class SupabaseModRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : ModRepository {
    
    override suspend fun getMod(modId: String): AuthResult<Mod?> {
        return try {
            val mod = client.from("mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", modId)
                    }
                }
                .decodeSingleOrNull<Mod>()
            
            AuthResult.Success(mod)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mod: ${e.message}", e)
        }
    }
    
    override suspend fun getModsByCarId(carId: String): AuthResult<List<Mod>> {
        return try {
            val mods = client.from("mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("car_id", carId)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            
            AuthResult.Success(mods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mods by car: ${e.message}", e)
        }
    }
    
    override suspend fun getRootMods(carId: String): AuthResult<List<Mod>> {
        return try {
            // Get all mods for the car and filter for null parent_id in Kotlin
            // This is more reliable than trying to use PostgREST null filtering
            val allModsResult = getModsByCarId(carId)
            if (allModsResult is AuthResult.Error) {
                return allModsResult
            }
            
            val allMods = (allModsResult as AuthResult.Success).data
            val rootMods = allMods.filter { it.parentId == null }
                .sortedBy { it.name }
            
            AuthResult.Success(rootMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get root mods: ${e.message}", e)
        }
    }
    
    override suspend fun getChildMods(parentId: String): AuthResult<List<Mod>> {
        return try {
            val mods = client.from("mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("parent_id", parentId)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            
            AuthResult.Success(mods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get child mods: ${e.message}", e)
        }
    }
    
    override suspend fun getModTree(carId: String): AuthResult<List<ModWithChildren>> {
        return try {
            // Get all mods for this car
            val allModsResult = getModsByCarId(carId)
            if (allModsResult is AuthResult.Error) {
                return allModsResult
            }
            
            val allMods = (allModsResult as AuthResult.Success).data
            
            // Build tree structure
            val modMap = allMods.associateBy { it.id }
            val rootMods = allMods.filter { it.parentId == null }
            
            fun buildTree(mod: Mod): ModWithChildren {
                val children = allMods
                    .filter { it.parentId == mod.id }
                    .map { buildTree(it) }
                return ModWithChildren(mod, children)
            }
            
            val tree = rootMods.map { buildTree(it) }
            AuthResult.Success(tree)
        } catch (e: Exception) {
            AuthResult.Error("Failed to build mod tree: ${e.message}", e)
        }
    }
}

