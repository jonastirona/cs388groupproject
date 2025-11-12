package com.example.modmycar

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.from
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

private fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

interface CarModRepository {
    suspend fun getCarMods(carId: String): AuthResult<List<CarMod>>
    suspend fun getCarMod(carId: String, modId: String): AuthResult<CarMod?>
    suspend fun getModsWithStatus(carId: String): AuthResult<List<ModWithStatus>> // All mods with completion status
    suspend fun getModTreeWithStatus(carId: String): AuthResult<List<ModWithStatus>> // Mod tree with status
    suspend fun markModCompleted(carId: String, modId: String, notes: String? = null): AuthResult<CarMod>
    suspend fun markModIncomplete(carId: String, modId: String): AuthResult<CarMod>
    suspend fun updateCarMod(carId: String, modId: String, update: CarModUpdate): AuthResult<CarMod>
    suspend fun deleteCarMod(carId: String, modId: String): AuthResult<Unit>
}

class SupabaseCarModRepository(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val modRepository: ModRepository = SupabaseModRepository()
) : CarModRepository {

    override suspend fun getCarMods(carId: String): AuthResult<List<CarMod>> {
        return try {
            val carMods = supabaseClient.from("car_mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("car_id", carId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CarMod>()
            AuthResult.Success(carMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get car mods: ${e.message}", e)
        }
    }

    override suspend fun getCarMod(carId: String, modId: String): AuthResult<CarMod?> {
        return try {
            val carMod = supabaseClient.from("car_mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("car_id", carId)
                        eq("mod_id", modId)
                    }
                }
                .decodeSingleOrNull<CarMod>()
            AuthResult.Success(carMod)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get car mod: ${e.message}", e)
        }
    }

    override suspend fun getModsWithStatus(carId: String): AuthResult<List<ModWithStatus>> {
        return try {
            // Get all mods
            val allModsResult = modRepository.getAllMods()
            if (allModsResult is AuthResult.Error) {
                return allModsResult
            }
            val allMods = (allModsResult as AuthResult.Success).data
            
            // Get car mods (completed mods for this car)
            val carModsResult = getCarMods(carId)
            if (carModsResult is AuthResult.Error) {
                return carModsResult
            }
            val carMods = (carModsResult as AuthResult.Success).data
            val carModMap = carMods.associateBy { it.modId }
            
            // Build map of completed mod IDs for unlock checking
            val completedModIds = carMods
                .filter { it.completedAt != null }
                .map { it.modId }
                .toSet()
            
            // Build ModWithStatus list
            val modsWithStatus = allMods.map { mod ->
                val carMod = carModMap[mod.id]
                val isCompleted = carMod?.completedAt != null
                
                // Check if mod is unlocked (parent mod is completed or mod has no parent)
                val isUnlocked = mod.parentModId == null || 
                    (mod.parentModId in completedModIds)
                
                ModWithStatus(
                    mod = mod,
                    carMod = carMod,
                    isCompleted = isCompleted,
                    isUnlocked = isUnlocked
                )
            }
            
            AuthResult.Success(modsWithStatus)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mods with status: ${e.message}", e)
        }
    }

    override suspend fun getModTreeWithStatus(carId: String): AuthResult<List<ModWithStatus>> {
        return try {
            // Get mod tree with status
            val modsWithStatusResult = getModsWithStatus(carId)
            if (modsWithStatusResult is AuthResult.Error) {
                return modsWithStatusResult
            }
            val modsWithStatus = (modsWithStatusResult as AuthResult.Success).data
            
            // Return root mods with status (flat list)
            val rootMods = modsWithStatus.filter { it.mod.parentModId == null }
            AuthResult.Success(rootMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mod tree with status: ${e.message}", e)
        }
    }
    
    /**
     * Get mod tree with status in a hierarchical structure.
     * This is a helper method that builds a tree structure with children.
     */
    suspend fun getModTreeWithStatusHierarchical(carId: String): AuthResult<List<ModWithStatusTree>> {
        return try {
            // Get all mods with status
            val modsWithStatusResult = getModsWithStatus(carId)
            if (modsWithStatusResult is AuthResult.Error) {
                return AuthResult.Error("Failed to get mods with status: ${(modsWithStatusResult as AuthResult.Error).message}", null)
            }
            val modsWithStatus = (modsWithStatusResult as AuthResult.Success).data
            
            // Build tree structure
            fun buildTree(modWithStatus: ModWithStatus): ModWithStatusTree {
                val children = modsWithStatus
                    .filter { it.mod.parentModId == modWithStatus.mod.id }
                    .map { buildTree(it) }
                
                return ModWithStatusTree(
                    mod = modWithStatus.mod,
                    carMod = modWithStatus.carMod,
                    isCompleted = modWithStatus.isCompleted,
                    isUnlocked = modWithStatus.isUnlocked,
                    children = children
                )
            }
            
            // Get root mods and build tree
            val rootMods = modsWithStatus.filter { it.mod.parentModId == null }
            val tree = rootMods.map { buildTree(it) }
            
            AuthResult.Success(tree)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mod tree with status: ${e.message}", e)
        }
    }

    override suspend fun markModCompleted(carId: String, modId: String, notes: String?): AuthResult<CarMod> {
        return try {
            // Check if car_mod entry already exists
            val existingResult = getCarMod(carId, modId)
            if (existingResult is AuthResult.Error) {
                return existingResult
            }
            
            val existing = (existingResult as AuthResult.Success).data
            val now = getCurrentTimestamp()
            
            if (existing != null) {
                // Update existing entry
                val updateMap = buildMap {
                    put("completed_at", now)
                    if (notes != null) put("notes", notes)
                    put("updated_at", now)
                }
                
                val updated = supabaseClient.from("car_mods")
                    .update(updateMap) {
                        filter {
                            eq("car_id", carId)
                            eq("mod_id", modId)
                        }
                        select(Columns.ALL)
                    }
                    .decodeSingle<CarMod>()
                AuthResult.Success(updated)
            } else {
                // Create new entry
                val carMod = CarMod(
                    id = UUID.randomUUID().toString(),
                    carId = carId,
                    modId = modId,
                    completedAt = now,
                    notes = notes,
                    createdAt = now,
                    updatedAt = null
                )
                
                val created = supabaseClient.from("car_mods")
                    .insert(carMod) {
                        select(Columns.ALL)
                    }
                    .decodeSingle<CarMod>()
                AuthResult.Success(created)
            }
        } catch (e: Exception) {
            AuthResult.Error("Failed to mark mod as completed: ${e.message}", e)
        }
    }

    override suspend fun markModIncomplete(carId: String, modId: String): AuthResult<CarMod> {
        return try {
            val now = getCurrentTimestamp()
            val updateMap = buildMap {
                put("completed_at", null)
                put("updated_at", now)
            }
            
            val updated = supabaseClient.from("car_mods")
                .update(updateMap) {
                    filter {
                        eq("car_id", carId)
                        eq("mod_id", modId)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<CarMod>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to mark mod as incomplete: ${e.message}", e)
        }
    }

    override suspend fun updateCarMod(carId: String, modId: String, update: CarModUpdate): AuthResult<CarMod> {
        return try {
            val now = getCurrentTimestamp()
            val updateMap = buildMap {
                if (update.completedAt != null) {
                    put("completed_at", update.completedAt)
                } else if (update.completedAt == null && update.notes == null) {
                    // If explicitly setting completed_at to null, set it
                    put("completed_at", null)
                }
                if (update.notes != null) put("notes", update.notes)
                put("updated_at", now)
            }
            
            val updated = supabaseClient.from("car_mods")
                .update(updateMap) {
                    filter {
                        eq("car_id", carId)
                        eq("mod_id", modId)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<CarMod>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update car mod: ${e.message}", e)
        }
    }

    override suspend fun deleteCarMod(carId: String, modId: String): AuthResult<Unit> {
        return try {
            supabaseClient.from("car_mods")
                .delete {
                    filter {
                        eq("car_id", carId)
                        eq("mod_id", modId)
                    }
                }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete car mod: ${e.message}", e)
        }
    }
}

