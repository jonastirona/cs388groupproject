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

interface ModRepository {
    suspend fun getAllMods(): AuthResult<List<Mod>>
    suspend fun getMod(modId: String): AuthResult<Mod?>
    suspend fun getModsByCategory(category: String): AuthResult<List<Mod>>
    suspend fun getRootMods(): AuthResult<List<Mod>> // Mods with no parent
    suspend fun getChildMods(parentModId: String): AuthResult<List<Mod>>
    suspend fun getModTree(): AuthResult<List<ModWithChildren>> // Full tree structure
    suspend fun createMod(mod: ModCreate): AuthResult<Mod>
    suspend fun updateMod(modId: String, update: ModUpdate): AuthResult<Mod>
    suspend fun deleteMod(modId: String): AuthResult<Unit>
}

class SupabaseModRepository(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : ModRepository {

    override suspend fun getAllMods(): AuthResult<List<Mod>> {
        return try {
            val mods = supabaseClient.from("mods")
                .select(columns = Columns.ALL) {
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            AuthResult.Success(mods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mods: ${e.message}", e)
        }
    }

    override suspend fun getMod(modId: String): AuthResult<Mod?> {
        return try {
            val mod = supabaseClient.from("mods")
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

    override suspend fun getModsByCategory(category: String): AuthResult<List<Mod>> {
        return try {
            val mods = supabaseClient.from("mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("category", category)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            AuthResult.Success(mods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mods by category: ${e.message}", e)
        }
    }

    override suspend fun getRootMods(): AuthResult<List<Mod>> {
        return try {
            // Fetch all mods and filter for root mods (parent_mod_id is null)
            // This is more reliable than trying to filter null in PostgREST
            val allModsResult = getAllMods()
            if (allModsResult is AuthResult.Error) {
                return allModsResult
            }
            val allMods = (allModsResult as AuthResult.Success).data
            val rootMods = allMods.filter { it.parentModId == null }
            AuthResult.Success(rootMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get root mods: ${e.message}", e)
        }
    }

    override suspend fun getChildMods(parentModId: String): AuthResult<List<Mod>> {
        return try {
            val mods = supabaseClient.from("mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("parent_mod_id", parentModId)
                    }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            AuthResult.Success(mods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get child mods: ${e.message}", e)
        }
    }

    override suspend fun getModTree(): AuthResult<List<ModWithChildren>> {
        return try {
            // Get all mods first
            val allMods = supabaseClient.from("mods")
                .select(columns = Columns.ALL) {
                    order("name", Order.ASCENDING)
                }
                .decodeList<Mod>()
            
            // Build tree structure
            val rootMods = allMods.filter { it.parentModId == null }
            
            fun buildTree(mod: Mod): ModWithChildren {
                val children = allMods
                    .filter { it.parentModId == mod.id }
                    .map { buildTree(it) }
                return ModWithChildren(mod, children)
            }
            
            val tree = rootMods.map { buildTree(it) }
            AuthResult.Success(tree)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get mod tree: ${e.message}", e)
        }
    }

    override suspend fun createMod(mod: ModCreate): AuthResult<Mod> {
        return try {
            val modWithId = Mod(
                id = UUID.randomUUID().toString(),
                name = mod.name,
                description = mod.description,
                parentModId = mod.parentModId,
                category = mod.category,
                imageUrl = mod.imageUrl,
                createdAt = getCurrentTimestamp(),
                updatedAt = null
            )
            
            val created = supabaseClient.from("mods")
                .insert(modWithId) {
                    select(Columns.ALL)
                }
                .decodeSingle<Mod>()
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error("Failed to create mod: ${e.message}", e)
        }
    }

    override suspend fun updateMod(modId: String, update: ModUpdate): AuthResult<Mod> {
        return try {
            val updateMap = buildMap {
                if (update.name != null) put("name", update.name)
                if (update.description != null) put("description", update.description)
                if (update.parentModId != null) put("parent_mod_id", update.parentModId)
                if (update.category != null) put("category", update.category)
                if (update.imageUrl != null) put("image_url", update.imageUrl)
                put("updated_at", getCurrentTimestamp())
            }
            
            val updated = supabaseClient.from("mods")
                .update(updateMap) {
                    filter {
                        eq("id", modId)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<Mod>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update mod: ${e.message}", e)
        }
    }

    override suspend fun deleteMod(modId: String): AuthResult<Unit> {
        return try {
            supabaseClient.from("mods")
                .delete {
                    filter {
                        eq("id", modId)
                    }
                }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete mod: ${e.message}", e)
        }
    }
}

