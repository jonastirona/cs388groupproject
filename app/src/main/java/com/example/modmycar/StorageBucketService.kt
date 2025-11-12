package com.example.modmycar

import io.github.jan.supabase.storage.storage

/**
 * Service class for handling Supabase Storage operations.
 * Provides methods to upload, delete, and get public URLs for files.
 */
class StorageBucketService(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    
    /**
     * Uploads a file to Supabase Storage and returns the public URL.
     * 
     * @param bucket The storage bucket name (e.g., "car-images", "mod-images", "car-mod-media")
     * @param path The path within the bucket (e.g., "user123/car456/main.jpg")
     * @param bytes The file content as ByteArray
     * @param contentType The MIME type (e.g., "image/jpeg", "video/mp4")
     * @return The public URL of the uploaded file
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String
    ): String {
        val storage = supabaseClient.storage.from(bucket)
        
        // Upload the file - use ByteArray directly
        storage.upload(
            path = path,
            data = bytes,
            options = {
                upsert = true // Overwrite if file exists
            }
        )
        
        // Get the public URL
        return storage.publicUrl(path)
    }
    
    /**
     * Deletes a file from Supabase Storage.
     * 
     * @param bucket The storage bucket name
     * @param path The path to the file to delete (can be full URL or just path)
     */
    suspend fun deleteFile(bucket: String, path: String) {
        val storage = supabaseClient.storage.from(bucket)
        
        // Extract path from URL if full URL is provided
        val filePath = if (path.startsWith("http")) {
            // Extract path from URL: https://...supabase.co/storage/v1/object/public/bucket/path
            path.substringAfter("/$bucket/")
        } else {
            path
        }
        
        // Use delete instead of remove
        storage.delete(listOf(filePath))
    }
    
    /**
     * Gets the public URL for a file in storage.
     * 
     * @param bucket The storage bucket name
     * @param path The path within the bucket
     * @return The public URL of the file
     */
    fun getPublicUrl(bucket: String, path: String): String {
        val storage = supabaseClient.storage.from(bucket)
        return storage.publicUrl(path)
    }
    
    /**
     * Uploads an image file to storage.
     * Helper method that sets the correct content type for images.
     */
    suspend fun uploadImage(
        bucket: String,
        path: String,
        bytes: ByteArray
    ): String {
        return uploadFile(bucket, path, bytes, "image/jpeg")
    }
    
    /**
     * Uploads a video file to storage.
     * Helper method that sets the correct content type for videos.
     */
    suspend fun uploadVideo(
        bucket: String,
        path: String,
        bytes: ByteArray
    ): String {
        return uploadFile(bucket, path, bytes, "video/mp4")
    }
}

