package com.example.modmycar

import io.github.jan.supabase.storage.storage

interface PostStorageService {
    suspend fun uploadPostMedia(
        userId: String,
        postId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String>
    
    suspend fun getPostMediaUrl(
        userId: String,
        postId: String,
        fileName: String
    ): AuthResult<String>
  
    suspend fun deletePostMedia(
        userId: String,
        postId: String,
        fileName: String
    ): AuthResult<Unit>
    
    suspend fun deleteAllPostMedia(
        userId: String,
        postId: String,
        fileNames: List<String>
    ): AuthResult<Unit>
}

class SupabasePostStorageService(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : PostStorageService {
    companion object {
        private const val POST_MEDIA_BUCKET = "post-media"
    }
    override suspend fun uploadPostMedia(
        userId: String,
        postId: String,
        fileName: String,
        fileBytes: ByteArray
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/post_$postId/$fileName"
            val storage = client.storage
            storage.from(POST_MEDIA_BUCKET)
                .upload(path, fileBytes) {
                    upsert = true
                }
            // Get public URL
            val publicUrl = storage.from(POST_MEDIA_BUCKET)
                .publicUrl(path)
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to upload post media: ${e.message}", e)
        }
    }
    override suspend fun getPostMediaUrl(
        userId: String,
        postId: String,
        fileName: String
    ): AuthResult<String> {
        return try {
            val path = "user_$userId/post_$postId/$fileName"
            val storage = client.storage
            
            val publicUrl = storage.from(POST_MEDIA_BUCKET)
                .publicUrl(path)
            
            AuthResult.Success(publicUrl)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get post media URL: ${e.message}", e)
        }
    }
    
    override suspend fun deletePostMedia(
        userId: String,
        postId: String,
        fileName: String
    ): AuthResult<Unit> {
        return try {
            val path = "user_$userId/post_$postId/$fileName"
            val storage = client.storage
            
            storage.from(POST_MEDIA_BUCKET)
                .delete(path)
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete post media: ${e.message}", e)
        }
    }
    
    override suspend fun deleteAllPostMedia(
        userId: String,
        postId: String,
        fileNames: List<String>
    ): AuthResult<Unit> {
        return try {
            val storage = client.storage
            
            val paths = fileNames.map { fileName ->
                "user_$userId/post_$postId/$fileName"
            }
            
            storage.from(POST_MEDIA_BUCKET)
                .delete(paths)
            
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete post media files: ${e.message}", e)
        }
    }
}