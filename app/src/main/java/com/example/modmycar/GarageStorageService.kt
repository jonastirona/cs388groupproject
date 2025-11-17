package com.example.modmycar

import io.github.jan.supabase.storage.storage

/**
 * Service for managing garage-related media files in Supabase Storage.
 * Handles upload, download, and deletion of files in garage-media and garage-mod-media buckets.
 */
interface GarageStorageService {
    // Garage Media (car images)
    suspend fun uploadGarageMedia(
        userId: String,
        garageCarId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String> // Returns public URL
    
    suspend fun getGarageMediaUrl(
        userId: String,
        garageCarId: String,
        fileName: String
    ): AuthResult<String> // Returns public URL
    
    suspend fun deleteGarageMedia(
        userId: String,
        garageCarId: String,
        fileName: String
    ): AuthResult<Unit>
    
    // Garage Mod Media
    suspend fun uploadGarageModMedia(
        userId: String,
        garageModId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String> // Returns public URL
    
    suspend fun getGarageModMediaUrl(
        userId: String,
        garageModId: String,
        fileName: String
    ): AuthResult<String> // Returns public URL
    
    suspend fun deleteGarageModMedia(
        userId: String,
        garageModId: String,
        fileName: String
    ): AuthResult<Unit>
}

class SupabaseGarageStorageService(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : GarageStorageService {
    
    private val GARAGE_MEDIA_BUCKET = "garage-media"
    private val GARAGE_MOD_MEDIA_BUCKET = "garage-mod-media"
    
    // Garage Media Methods
    override suspend fun uploadGarageMedia(
        userId: String,
        garageCarId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/car_$garageCarId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            storage.from(GARAGE_MEDIA_BUCKET)
                .upload(path, fileBytes) {
                    upsert = true
                }
            
            // Get public URL
            val publicUrl = storage.from(GARAGE_MEDIA_BUCKET)
                .publicUrl(path)
            
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to upload garage media: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageMediaUrl(
        userId: String,
        garageCarId: String,
        fileName: String
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/car_$garageCarId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            val publicUrl = storage.from(GARAGE_MEDIA_BUCKET)
                .publicUrl(path)
            
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage media URL: ${e.message}", e)
        }
    }
    
    override suspend fun deleteGarageMedia(
        userId: String,
        garageCarId: String,
        fileName: String
    ): AuthResult<Unit> {
        return try {
            val path = "user_$userId/car_$garageCarId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            storage.from(GARAGE_MEDIA_BUCKET)
                .delete(path)
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete garage media: ${e.message}", e)
        }
    }
    
    // Garage Mod Media Methods
    override suspend fun uploadGarageModMedia(
        userId: String,
        garageModId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/mod_$garageModId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            storage.from(GARAGE_MOD_MEDIA_BUCKET)
                .upload(path, fileBytes) {
                    upsert = true
                }
            
            // Get public URL
            val publicUrl = storage.from(GARAGE_MOD_MEDIA_BUCKET)
                .publicUrl(path)
            
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to upload garage mod media: ${e.message}", e)
        }
    }
    
    override suspend fun getGarageModMediaUrl(
        userId: String,
        garageModId: String,
        fileName: String
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/mod_$garageModId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            val publicUrl = storage.from(GARAGE_MOD_MEDIA_BUCKET)
                .publicUrl(path)
            
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get garage mod media URL: ${e.message}", e)
        }
    }
    
    override suspend fun deleteGarageModMedia(
        userId: String,
        garageModId: String,
        fileName: String
    ): AuthResult<Unit> {
        return try {
            val path = "user_$userId/mod_$garageModId/$fileName"
            val storage = client.storage
                ?: return AuthResult.Error("Storage module not initialized", null)
            
            storage.from(GARAGE_MOD_MEDIA_BUCKET)
                .delete(path)
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete garage mod media: ${e.message}", e)
        }
    }
}

