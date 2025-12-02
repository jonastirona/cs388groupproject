package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

interface GarageCarRepository {
    suspend fun createGarageCar(garageCar: GarageCarCreate): AuthResult<GarageCar>
    suspend fun getGarageCar(garageCarId: String): AuthResult<GarageCar?>
    suspend fun getGarageCarsByUserId(userId: String): AuthResult<List<GarageCar>>
    suspend fun getNearbyGarageCars(latitude: Double, longitude: Double, radiusMiles: Double = 15.0): AuthResult<List<NearbyGarageCar>>
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
    
    override suspend fun getNearbyGarageCars(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double
    ): AuthResult<List<NearbyGarageCar>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Fetch all garage cars
            val garageCars = client.from("garage_cars")
                .select(columns = Columns.ALL)
                .decodeList<GarageCar>()
            
            // Fetch all user profiles (we'll filter for location data in Kotlin)
            val allProfiles = client.from("profiles")
                .select(columns = Columns.ALL)
                .decodeList<UserProfile>()
            
            // Filter to only profiles with location data
            val profiles = allProfiles.filter { 
                it.latitude != null && it.longitude != null 
            }
            
            // Create a map of userId -> profile for quick lookup
            val profileMap = profiles.associateBy { it.id }
            
            // Join garage cars with profiles and calculate distances
            val nearbyCars = mutableListOf<NearbyGarageCar>()
            
            for (garageCar in garageCars) {
                try {
                    val profile = profileMap[garageCar.userId] ?: continue
                    val userLat = profile.latitude ?: continue
                    val userLon = profile.longitude ?: continue
                    
                    // Calculate distance in miles
                    val distanceMiles = calculateDistanceMiles(latitude, longitude, userLat, userLon)
                    
                    // Filter by radius
                    if (distanceMiles <= radiusMiles) {
                        nearbyCars.add(NearbyGarageCar(garageCar, profile, distanceMiles))
                    }
                } catch (e: Exception) {
                    // Skip this car if there's an error
                }
            }
            
            val sortedCars = nearbyCars.sortedBy { it.distanceMiles }
            
            AuthResult.Success(sortedCars)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get nearby garage cars: ${e.message}", e)
        }
    }
    
    /**
     * Calculates the distance between two points on Earth using the Haversine formula.
     * Returns distance in miles.
     */
    private fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMiles = 3958.8 // Earth's radius in miles
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusMiles * c
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

