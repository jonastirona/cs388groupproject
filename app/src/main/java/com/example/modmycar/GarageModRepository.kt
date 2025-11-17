package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface GarageModRepository {
    suspend fun createGarageMod(garageMod: GarageModCreate): AuthResult<GarageMod>
    suspend fun getGarageMod(garageModId: String): AuthResult<GarageMod?>
    suspend fun getGarageModsByUserId(userId: String): AuthResult<List<GarageMod>>
    suspend fun getGarageModsByModId(modId: String): AuthResult<List<GarageMod>>
    suspend fun updateGarageMod(garageModId: String, update: GarageModUpdate): AuthResult<GarageMod>
    suspend fun deleteGarageMod(garageModId: String): AuthResult<Unit>
}

class SupabaseGarageModRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : GarageModRepository {
    
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    
    override suspend fun createGarageMod(garageMod: GarageModCreate): AuthResult<GarageMod> {
        return try {
            val created = client.from("garage_mods")
                .insert(garageMod) {
                    select(columns = Columns.ALL)
                }
                .decodeSingle<GarageMod>()
            
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error("Failed to create garage mod: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageMod(garageModId: String): AuthResult<GarageMod?> {
        return try {
            val garageMod = client.from("garage_mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", garageModId)
                    }
                }
                .decodeSingleOrNull<GarageMod>()
            
            AuthResult.Success(garageMod)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage mod: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageModsByUserId(userId: String): AuthResult<List<GarageMod>> {
        return try {
            val garageMods = client.from("garage_mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("completed_at", Order.DESCENDING)
                }
                .decodeList<GarageMod>()
            
            AuthResult.Success(garageMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage mods: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageModsByModId(modId: String): AuthResult<List<GarageMod>> {
        return try {
            val garageMods = client.from("garage_mods")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("mod_id", modId)
                    }
                    order("completed_at", Order.DESCENDING)
                }
                .decodeList<GarageMod>()
            
            AuthResult.Success(garageMods)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage mods by mod id: ${e.message}", e)
        }
    }
    
    override suspend fun updateGarageMod(garageModId: String, update: GarageModUpdate): AuthResult<GarageMod> {
        return try {
            val updated = client.from("garage_mods")
                .update(update) {
                    filter {
                        eq("id", garageModId)
                    }
                    select(columns = Columns.ALL)
                }
                .decodeSingle<GarageMod>()
            
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update garage mod: ${e.message}", e)
        }
    }
    
    override suspend fun deleteGarageMod(garageModId: String): AuthResult<Unit> {
        return try {
            client.from("garage_mods")
                .delete {
                    filter {
                        eq("id", garageModId)
                    }
                }
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete garage mod: ${e.message}", e)
        }
    }
}

