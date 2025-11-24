package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

interface CarRepository {
    suspend fun getCar(carId: String): AuthResult<Car?>
    suspend fun getAllCars(): AuthResult<List<Car>>
    suspend fun getAllMakes(): AuthResult<List<String>>
    suspend fun getModelsByMake(make: String): AuthResult<List<String>>
    suspend fun getMakesWithModels(): AuthResult<Map<String, List<String>>>
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
    
    /**
     * Gets all unique makes from the cars table.
     */
    override suspend fun getAllMakes(): AuthResult<List<String>> {
        return try {
            val cars = client.from("cars")
                .select(columns = Columns.ALL) {
                    order("make", Order.ASCENDING)
                }
                .decodeList<Car>()
            
            // Extract unique makes
            val makes = cars.map { it.make }.distinct()
            AuthResult.Success(makes)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get makes: ${e.message}", e)
        }
    }
    
    /**
     * Gets all models for a specific make.
     */
    override suspend fun getModelsByMake(make: String): AuthResult<List<String>> {
        return try {
            val cars = client.from("cars")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("make", make)
                    }
                    order("model", Order.ASCENDING)
                }
                .decodeList<Car>()
            
            // Extract unique models for this make
            val models = cars.map { it.model }.distinct()
            AuthResult.Success(models)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get models for make $make: ${e.message}", e)
        }
    }
    
    /**
     * Gets all makes with their associated models grouped together.
     * Returns a map where keys are makes and values are lists of models.
     */
    override suspend fun getMakesWithModels(): AuthResult<Map<String, List<String>>> {
        return try {
            val cars = client.from("cars")
                .select(columns = Columns.ALL) {
                    order("make", Order.ASCENDING)
                    order("model", Order.ASCENDING)
                }
                .decodeList<Car>()
            
            // Group models by make
            val makesWithModels = cars
                .groupBy { it.make }
                .mapValues { (_, cars) -> cars.map { it.model }.distinct() }
            
            AuthResult.Success(makesWithModels)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get makes with models: ${e.message}", e)
        }
    }
}

