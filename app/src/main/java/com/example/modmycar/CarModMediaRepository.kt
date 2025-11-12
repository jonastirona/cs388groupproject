package com.example.modmycar

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

interface CarModMediaRepository {
    suspend fun getCarModMedia(carModId: String): AuthResult<List<CarModMedia>>
    suspend fun getCarModMediaById(mediaId: String): AuthResult<CarModMedia?>
    suspend fun addCarModMedia(media: CarModMediaCreate): AuthResult<CarModMedia>
    suspend fun deleteCarModMedia(mediaId: String): AuthResult<Unit>
    suspend fun uploadImage(carModId: String, imageBytes: ByteArray, filename: String): AuthResult<CarModMedia>
    suspend fun uploadVideo(carModId: String, videoBytes: ByteArray, filename: String): AuthResult<CarModMedia>
}

class SupabaseCarModMediaRepository(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client,
    private val storageService: StorageBucketService = StorageBucketService()
) : CarModMediaRepository {

    override suspend fun getCarModMedia(carModId: String): AuthResult<List<CarModMedia>> {
        return try {
            val media = supabaseClient.from("car_mod_media")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("car_mod_id", carModId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CarModMedia>()
            AuthResult.Success(media)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get car mod media: ${e.message}", e)
        }
    }

    override suspend fun getCarModMediaById(mediaId: String): AuthResult<CarModMedia?> {
        return try {
            val media = supabaseClient.from("car_mod_media")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", mediaId)
                    }
                }
                .decodeSingleOrNull<CarModMedia>()
            AuthResult.Success(media)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get car mod media: ${e.message}", e)
        }
    }

    override suspend fun addCarModMedia(media: CarModMediaCreate): AuthResult<CarModMedia> {
        return try {
            val mediaWithId = CarModMedia(
                id = UUID.randomUUID().toString(),
                carModId = media.carModId,
                storageUrl = media.storageUrl,
                mediaType = media.mediaType,
                thumbnailUrl = media.thumbnailUrl,
                createdAt = getCurrentTimestamp()
            )
            
            val created = supabaseClient.from("car_mod_media")
                .insert(mediaWithId) {
                    select(Columns.ALL)
                }
                .decodeSingle<CarModMedia>()
            AuthResult.Success(created)
        } catch (e: Exception) {
            AuthResult.Error("Failed to add car mod media: ${e.message}", e)
        }
    }

    override suspend fun deleteCarModMedia(mediaId: String): AuthResult<Unit> {
        return try {
            // First get the media to get the storage URL for deletion
            val mediaResult = getCarModMediaById(mediaId)
            if (mediaResult is AuthResult.Success && mediaResult.data != null) {
                val media = mediaResult.data
                // Delete from storage
                storageService.deleteFile("car-mod-media", media.storageUrl)
            }
            
            // Delete from database
            supabaseClient.from("car_mod_media")
                .delete {
                    filter {
                        eq("id", mediaId)
                    }
                }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete car mod media: ${e.message}", e)
        }
    }

    override suspend fun uploadImage(carModId: String, imageBytes: ByteArray, filename: String): AuthResult<CarModMedia> {
        return try {
            // Generate unique path for the image
            val path = "car-mod-media/$carModId/${UUID.randomUUID()}_$filename"
            
            // Upload to storage
            val storageUrl = storageService.uploadFile(
                bucket = "car-mod-media",
                path = path,
                bytes = imageBytes,
                contentType = "image/jpeg"
            )
            
            // Create database entry
            val media = CarModMediaCreate(
                carModId = carModId,
                storageUrl = storageUrl,
                mediaType = "image",
                thumbnailUrl = null
            )
            
            addCarModMedia(media)
        } catch (e: Exception) {
            AuthResult.Error("Failed to upload image: ${e.message}", e)
        }
    }

    override suspend fun uploadVideo(carModId: String, videoBytes: ByteArray, filename: String): AuthResult<CarModMedia> {
        return try {
            // Generate unique path for the video
            val path = "car-mod-media/$carModId/${UUID.randomUUID()}_$filename"
            
            // Upload to storage
            val storageUrl = storageService.uploadFile(
                bucket = "car-mod-media",
                path = path,
                bytes = videoBytes,
                contentType = "video/mp4"
            )
            
            // For videos, we could generate a thumbnail, but for now we'll leave it null
            // You can implement thumbnail generation later if needed
            
            // Create database entry
            val media = CarModMediaCreate(
                carModId = carModId,
                storageUrl = storageUrl,
                mediaType = "video",
                thumbnailUrl = null // Can be set later if thumbnail generation is implemented
            )
            
            addCarModMedia(media)
        } catch (e: Exception) {
            AuthResult.Error("Failed to upload video: ${e.message}", e)
        }
    }
}

