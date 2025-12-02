package com.example.modmycar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for tracking and updating user location in the database.
 */
class LocationTrackingService(
    private val context: Context,
    private val userRepository: UserRepository = SupabaseUserRepository(),
    private val authRepository: AuthRepository = SupabaseAuthRepository()
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Updates the current user's location in the database.
     * Returns true if location was successfully updated, false otherwise.
     */
    suspend fun updateUserLocation(): Boolean {
        // Check location permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        // Get current user
        val userResult = authRepository.getCurrentSession()
        val userId = when (userResult) {
            is AuthResult.Success -> userResult.data?.id
            is AuthResult.Error -> return false
        }
        
        if (userId == null) {
            return false
        }
        
        // Get current location - try lastLocation first, then request fresh location
        return try {
            // First try to get last known location (fast)
            var location = try {
                awaitTask(fusedLocationClient.lastLocation)
            } catch (e: Exception) {
                null
            }
            
            // If lastLocation is null or too old (older than 5 minutes), request fresh location
            if (location == null || (System.currentTimeMillis() - location.time > 5 * 60 * 1000)) {
                location = requestFreshLocation()
            }
            
            if (location != null) {
                updateLocationInDatabase(userId, location.latitude, location.longitude)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the user's location in the database.
     */
    private suspend fun updateLocationInDatabase(
        userId: String,
        latitude: Double,
        longitude: Double
    ): Boolean {
        return try {
            when (val result = userRepository.updateLocation(userId, latitude, longitude)) {
                is AuthResult.Success -> true
                is AuthResult.Error -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the current location as a LatLng pair.
     * Returns null if location cannot be obtained.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            // First try to get last known location (fast)
            var location = awaitTask(fusedLocationClient.lastLocation)

            // If lastLocation is null or too old, request fresh location
            if (location == null || (System.currentTimeMillis() - location.time > 5 * 60 * 1000)) {
                location = requestFreshLocation()
            }

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Requests a fresh location update.
     */
    private suspend fun requestFreshLocation(): Location? {
        return try {
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000)
                .setMaxUpdateAgeMillis(5000)
                .build()

            val cancellationTokenSource = CancellationTokenSource()
            awaitTask<Location>(
                fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a Google Play Services Task to a suspend function.
     */
    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                cont.resume(completedTask.result)
            } else {
                val exception = completedTask.exception
                    ?: Exception("Task failed with unknown error")
                cont.resumeWithException(exception)
            }
        }
    }

}

