package com.example.modmycar

import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Service for retrieving car images from the car-images storage bucket.
 * Images are named by make (e.g., "Honda.png", "BMW.png").
 * Uses signed URLs for private bucket access with caching to improve performance.
 */
object CarImageService {
    private const val TAG = "CarImageService"
    private const val CAR_IMAGES_BUCKET = "car-images"
    private val SIGNED_URL_EXPIRY: Duration = 24.hours // 24 hours for better caching
    private val URL_CACHE_BUFFER: Duration = 1.hours // Regenerate 1 hour before expiry
    
    // In-memory cache for signed URLs with expiry tracking
    private data class CachedUrl(
        val url: String,
        val expiresAt: TimeMark
    )
    
    private val urlCache = mutableMapOf<String, CachedUrl>()
    private val timeSource = TimeSource.Monotonic
    
    /**
     * Gets a signed URL for a car image based on the make.
     * Note: This is a blocking call that should be used carefully.
     * For better performance, consider using getCarImageUrlSuspend() instead.
     * @param make The car make (e.g., "Honda", "BMW")
     * @return The signed URL to the car image, or null if storage is not available
     */
    fun getCarImageUrl(make: String): String? {
        return runBlocking {
            getCarImageUrlSuspend(make)
        }
    }
    
    /**
     * Gets a signed URL for a car image based on the make (suspend version).
     * Uses caching to avoid regenerating URLs unnecessarily.
     * @param make The car make (e.g., "Honda", "BMW")
     * @return The signed URL to the car image, or null if storage is not available
     */
    suspend fun getCarImageUrlSuspend(make: String): String? {
        return try {
            // Check cache first
            val cached = urlCache[make]
            if (cached != null && cached.expiresAt.hasNotPassedNow()) {
                Log.d(TAG, "Using cached URL for make: '$make'")
                return cached.url
            }
            
            Log.d(TAG, "Generating new signed URL for make: '$make'")
            
            val storage = SupabaseClient.client.storage
            if (storage == null) {
                Log.e(TAG, "Storage module is null - storage not initialized")
                return null
            }
            
            val fileName = "$make.png"
            Log.d(TAG, "Constructed filename: '$fileName' for bucket: '$CAR_IMAGES_BUCKET'")
            
            // Create signed URL for private bucket access
            val signedUrl = storage.from(CAR_IMAGES_BUCKET)
                .createSignedUrl(fileName, expiresIn = SIGNED_URL_EXPIRY)
            
            // Cache the URL with expiry tracking (subtract buffer to regenerate before expiry)
            val expiresAt = timeSource.markNow() + SIGNED_URL_EXPIRY - URL_CACHE_BUFFER
            urlCache[make] = CachedUrl(signedUrl, expiresAt)
            
            Log.d(TAG, "Generated and cached signed URL for '$make', expires in ${SIGNED_URL_EXPIRY.inWholeHours} hours")
            signedUrl
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting car image URL for make '$make': ${e.message}", e)
            null
        }
    }
    
    /**
     * Clears the URL cache. Call this when a new car is added to ensure fresh URLs.
     */
    fun clearCache() {
        Log.d(TAG, "Clearing URL cache")
        urlCache.clear()
    }
    
    /**
     * Clears the cached URL for a specific make.
     */
    fun clearCacheForMake(make: String) {
        Log.d(TAG, "Clearing URL cache for make: '$make'")
        urlCache.remove(make)
    }
}

