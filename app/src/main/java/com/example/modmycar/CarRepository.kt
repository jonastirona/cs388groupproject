package com.example.modmycar

import android.util.Log
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

interface CarRepository {
    suspend fun getCarsByUser(userId: String): AuthResult<List<Car>>
    suspend fun getCar(carId: String): AuthResult<Car?>
    suspend fun createCar(car: CarCreate): AuthResult<Car>
    suspend fun updateCar(carId: String, update: CarUpdate): AuthResult<Car>
    suspend fun deleteCar(carId: String): AuthResult<Unit>
    suspend fun updateCarImage(carId: String, imageUrl: String): AuthResult<Car>
}

class SupabaseCarRepository(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : CarRepository {
    
    private val TAG = "CarRepository"

    override suspend fun getCarsByUser(userId: String): AuthResult<List<Car>> {
        return try {
            val cars = supabaseClient.from("cars")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Car>()
            AuthResult.Success(cars)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get cars: ${e.message}", e)
        }
    }

    override suspend fun getCar(carId: String): AuthResult<Car?> {
        return try {
            val car = supabaseClient.from("cars")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", carId)
                    }
                }
                .decodeSingleOrNull<Car>()
            AuthResult.Success(car)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get car: ${e.message}", e)
        }
    }

    override suspend fun createCar(car: CarCreate): AuthResult<Car> {
        return try {
            // Generate UUID for the car
            val carWithId = Car(
                id = UUID.randomUUID().toString(),
                userId = car.userId,
                make = car.make,
                model = car.model,
                color = car.color,
                year = car.year,
                imageUrl = car.imageUrl,
                createdAt = getCurrentTimestamp(),
                updatedAt = null
            )
            
            Log.d(TAG, "DEBUG: Attempting to insert car with data:")
            Log.d(TAG, "  ID: ${carWithId.id}")
            Log.d(TAG, "  User ID: ${carWithId.userId}")
            Log.d(TAG, "  Make: ${carWithId.make}")
            Log.d(TAG, "  Model: ${carWithId.model}")
            Log.d(TAG, "  Color: ${carWithId.color}")
            Log.d(TAG, "  Year: ${carWithId.year}")
            Log.d(TAG, "  Image URL: ${carWithId.imageUrl}")
            Log.d(TAG, "  Created At: ${carWithId.createdAt}")
            
            val created = supabaseClient.from("cars")
                .insert(carWithId) {
                    select(Columns.ALL)
                }
                .decodeSingle<Car>()
            AuthResult.Success(created)
        } catch (e: Exception) {
            // Build detailed error message
            val errorDetails = buildString {
                append("Failed to create car: ${e.message}")
                append("\nException Type: ${e.javaClass.name}")
                
                // Check for nested exceptions
                var cause = e.cause
                var depth = 0
                while (cause != null && depth < 5) {
                    append("\nCaused by (depth $depth): ${cause.javaClass.name}: ${cause.message}")
                    cause = cause.cause
                    depth++
                }
                
                // Try to extract Supabase-specific error details
                val exceptionString = e.toString()
                if (exceptionString.contains("HttpResponseException") || 
                    exceptionString.contains("PostgrestException") ||
                    exceptionString.contains("SupabaseException")) {
                    append("\nSupabase Error Details: $exceptionString")
                }
                
                // Include stack trace (first 10 lines)
                val stackTrace = e.stackTrace.take(10).joinToString("\n") { 
                    "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})"
                }
                append("\nStack Trace (first 10 lines):\n$stackTrace")
            }
            
            // Log error for debugging
            Log.e(TAG, "ERROR: $errorDetails")
            Log.e(TAG, "Exception stack trace:", e)
            
            AuthResult.Error(errorDetails, e)
        }
    }

    override suspend fun updateCar(carId: String, update: CarUpdate): AuthResult<Car> {
        return try {
            // Build update object with only non-null fields
            val updateMap = buildMap {
                if (update.make != null) put("make", update.make)
                if (update.model != null) put("model", update.model)
                if (update.color != null) put("color", update.color)
                if (update.year != null) put("year", update.year)
                if (update.imageUrl != null) put("image_url", update.imageUrl)
                put("updated_at", getCurrentTimestamp())
            }
            
            val updated = supabaseClient.from("cars")
                .update(updateMap) {
                    filter {
                        eq("id", carId)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<Car>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update car: ${e.message}", e)
        }
    }

    override suspend fun deleteCar(carId: String): AuthResult<Unit> {
        return try {
            supabaseClient.from("cars")
                .delete {
                    filter {
                        eq("id", carId)
                    }
                }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete car: ${e.message}", e)
        }
    }

    override suspend fun updateCarImage(carId: String, imageUrl: String): AuthResult<Car> {
        return try {
            val updateMap = buildMap {
                put("image_url", imageUrl)
                put("updated_at", getCurrentTimestamp())
            }
            
            val updated = supabaseClient.from("cars")
                .update(updateMap) {
                    filter {
                        eq("id", carId)
                    }
                    select(Columns.ALL)
                }
                .decodeSingle<Car>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update car image: ${e.message}", e)
        }
    }
}

