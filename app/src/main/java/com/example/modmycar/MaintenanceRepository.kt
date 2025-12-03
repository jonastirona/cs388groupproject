package com.example.modmycar

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface MaintenanceRepository {
    suspend fun createMaintenanceItem(item: MaintenanceItemCreate): AuthResult<MaintenanceItem>
    suspend fun getMaintenanceItemsByCarId(garageCarId: String): AuthResult<List<MaintenanceItem>>
    suspend fun updateMaintenanceItem(itemId: String, update: MaintenanceItemUpdate): AuthResult<MaintenanceItem>
    suspend fun deleteMaintenanceItem(itemId: String): AuthResult<Unit>
    suspend fun createDefaultMaintenanceItems(garageCarId: String): AuthResult<List<MaintenanceItem>>
}

class SupabaseMaintenanceRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : MaintenanceRepository {
    
    override suspend fun createMaintenanceItem(item: MaintenanceItemCreate): AuthResult<MaintenanceItem> {
        return try {
            val created = client.from("maintenance_records")
                .insert(item) {
                    select(columns = Columns.ALL)
                }
                .decodeSingle<MaintenanceItem>()
            
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error("Failed to create maintenance item: ${e.message}", e)
        }
    }
    
    override suspend fun getMaintenanceItemsByCarId(garageCarId: String): AuthResult<List<MaintenanceItem>> {
        return try {
            val items = client.from("maintenance_records")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("garage_car_id", garageCarId)
                    }
                }
                .decodeList<MaintenanceItem>()
            
            AuthResult.Success(items)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get maintenance items: ${e.message}", e)
        }
    }
    
    override suspend fun updateMaintenanceItem(itemId: String, update: MaintenanceItemUpdate): AuthResult<MaintenanceItem> {
        return try {
            val updated = client.from("maintenance_records")
                .update(update) {
                    filter {
                        eq("id", itemId)
                    }
                    select(columns = Columns.ALL)
                }
                .decodeSingle<MaintenanceItem>()
            
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Error("Failed to update maintenance item: ${e.message}", e)
        }
    }
    
    override suspend fun deleteMaintenanceItem(itemId: String): AuthResult<Unit> {
        return try {
            client.from("maintenance_records")
                .delete {
                    filter {
                        eq("id", itemId)
                    }
                }
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete maintenance item: ${e.message}", e)
        }
    }
    
    override suspend fun createDefaultMaintenanceItems(garageCarId: String): AuthResult<List<MaintenanceItem>> {
        return try {
            val createdItems = mutableListOf<MaintenanceItem>()
            
            for (type in MaintenanceType.entries) {
                val item = MaintenanceItemCreate(
                    garageCarId = garageCarId,
                    type = type.typeName,
                    lastServiceDate = null,
                    intervalDays = type.defaultIntervalDays
                )
                
                val created = client.from("maintenance_records")
                    .insert(item) {
                        select(columns = Columns.ALL)
                    }
                    .decodeSingle<MaintenanceItem>()
                
                createdItems.add(created)
            }
            
            AuthResult.Success(createdItems)
        } catch (e: Exception) {
            AuthResult.Error("Failed to create default maintenance items: ${e.message}", e)
        }
    }
}

