package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

interface CarRepository {
    suspend fun getCar(carId: String): AuthResult<Car?>
    suspend fun getAllCars(): AuthResult<List<Car>>
}

class SupabaseCarRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : CarRepository {
    
    override suspend fun getCar(carId: String): AuthResult<Car?> {
        return try {
            val car = client.from("cars")
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
    
    override suspend fun getAllCars(): AuthResult<List<Car>> {
        return try {
            val cars = client.from("cars")
                .select(columns = Columns.ALL) {
                    order("make", Order.ASCENDING)
                    order("model", Order.ASCENDING)
                }
                .decodeList<Car>()
            
            AuthResult.Success(cars)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get cars: ${e.message}", e)
        }
    }
}

