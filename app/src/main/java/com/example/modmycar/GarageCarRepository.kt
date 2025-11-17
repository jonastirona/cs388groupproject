package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface GarageCarRepository {
    suspend fun createGarageCar(garageCar: GarageCarCreate): AuthResult<GarageCar>
    suspend fun getGarageCar(garageCarId: String): AuthResult<GarageCar?>
    suspend fun getGarageCarsByUserId(userId: String): AuthResult<List<GarageCar>>
    suspend fun updateGarageCar(garageCarId: String, update: GarageCarUpdate): AuthResult<GarageCar>
    suspend fun deleteGarageCar(garageCarId: String): AuthResult<Unit>
}

class SupabaseGarageCarRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : GarageCarRepository {
    
    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    
    override suspend fun createGarageCar(garageCar: GarageCarCreate): AuthResult<GarageCar> {
        return try {
            val created = client.from("garage_cars")
                .insert(garageCar) {
                    select(columns = Columns.ALL)
                }
                .decodeSingle<GarageCar>()
            
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error("Failed to create garage car: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageCar(garageCarId: String): AuthResult<GarageCar?> {
        return try {
            val garageCar = client.from("garage_cars")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", garageCarId)
                    }
                }
                .decodeSingleOrNull<GarageCar>()
            
            AuthResult.Success(garageCar)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage car: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageCarsByUserId(userId: String): AuthResult<List<GarageCar>> {
        return try {
            val garageCars = client.from("garage_cars")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<GarageCar>()
            
            AuthResult.Success(garageCars)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage cars: ${e.message}", e)
        }
    }
    
    override suspend fun updateGarageCar(garageCarId: String, update: GarageCarUpdate): AuthResult<GarageCar> {
        return try {
            val updated = client.from("garage_cars")
                .update(update) {
                    filter {
                        eq("id", garageCarId)
                    }
                    select(columns = Columns.ALL)
                }
                .decodeSingle<GarageCar>()
            
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update garage car: ${e.message}", e)
        }
    }
    
    override suspend fun deleteGarageCar(garageCarId: String): AuthResult<Unit> {
        return try {
            client.from("garage_cars")
                .delete {
                    filter {
                        eq("id", garageCarId)
                    }
                }
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete garage car: ${e.message}", e)
        }
    }
}

