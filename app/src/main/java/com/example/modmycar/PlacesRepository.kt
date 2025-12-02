package com.example.modmycar

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface PlacesRepository {
    suspend fun searchNearbyShops(
        latitude: Double,
        longitude: Double,
        radius: Int = 5000
    ): AuthResult<List<Shop>>
    
    suspend fun getPlaceDetails(placeId: String): AuthResult<Shop?>
}

class GooglePlacesRepository(
    private val context: Context
) : PlacesRepository {
    
    init {
        // Initialize Places SDK if not already initialized
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
    
    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }
    
    override suspend fun searchNearbyShops(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): AuthResult<List<Shop>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Use Places API Nearby Search via REST API
            val shops = searchNearbyShopsRestApi(latitude, longitude, radius)
            AuthResult.Success(shops)
        } catch (e: Exception) {
            AuthResult.Error("Failed to search nearby shops: ${e.message}", e)
        }
    }
    
    private suspend fun searchNearbyShopsRestApi(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): List<Shop> {
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        require(apiKey.isNotBlank()) { "GOOGLE_MAPS_API_KEY is missing. Add it to local.properties." }
        
        val service = placesApiService
        val location = "$latitude,$longitude"
        
        val shops = mutableListOf<Shop>()
        
        // Use text search with automotive-related keywords and location bias
        // More specific queries to get better results
        val searchQueries = listOf(
            "auto repair",
            "car repair",
            "automotive repair",
            "car parts store",
            "auto parts",
            "automotive parts",
            "tuning shop",
            "performance shop",
            "auto body shop",
            "collision repair",
            "mechanic",
            "transmission repair",
            "exhaust shop",
            "brake shop"
        )
        
        for (query in searchQueries) {
            try {
                val response = service.searchText(
                    key = apiKey,
                    query = query,
                    location = location,
                    radius = radius
                )
                
                response.results?.forEach { result ->
                    // Filter to ensure it's actually automotive-related and within radius
                    if (isAutomotiveRelated(result) && isWithinRadius(
                        latitude, longitude,
                        result.geometry?.location?.lat ?: 0.0,
                        result.geometry?.location?.lng ?: 0.0,
                        radius
                    )) {
                        val shop = Shop(
                            id = result.placeId ?: "",
                            name = result.name ?: "Unknown",
                            address = result.vicinity ?: result.formattedAddress ?: "",
                            phone = null,
                            website = null,
                            rating = result.rating,
                            latitude = result.geometry?.location?.lat ?: 0.0,
                            longitude = result.geometry?.location?.lng ?: 0.0,
                            placeId = result.placeId ?: "",
                            openingHours = null,
                            types = result.types
                        )
                        shops.add(shop)
                    }
                }
            } catch (e: HttpException) {
                // Continue with other queries if one fails
                continue
            } catch (e: Exception) {
                // Continue on any error
                continue
            }
        }
        
        // Also try nearby search with specific place types as fallback
        val placeTypes = listOf("car_repair", "car_parts_store", "automotive_repair_shop")
        for (type in placeTypes) {
            try {
                val response = service.searchNearby(
                    key = apiKey,
                    location = location,
                    radius = radius,
                    type = type
                )
                
                response.results?.forEach { result ->
                    if (isAutomotiveRelated(result)) {
                        val shop = Shop(
                            id = result.placeId ?: "",
                            name = result.name ?: "Unknown",
                            address = result.vicinity ?: result.formattedAddress ?: "",
                            phone = null,
                            website = null,
                            rating = result.rating,
                            latitude = result.geometry?.location?.lat ?: 0.0,
                            longitude = result.geometry?.location?.lng ?: 0.0,
                            placeId = result.placeId ?: "",
                            openingHours = null,
                            types = result.types
                        )
                        shops.add(shop)
                    }
                }
            } catch (e: HttpException) {
                continue
            } catch (e: Exception) {
                continue
            }
        }
        
        return shops.distinctBy { it.placeId }
    }
    
    private fun isAutomotiveRelated(result: PlaceResult): Boolean {
        val name = (result.name ?: "").lowercase().trim()
        val types = result.types ?: emptyList()
        val address = (result.vicinity ?: result.formattedAddress ?: "").lowercase()
        
        // STRICT: Must have automotive-specific place types (exclude generic ones)
        val requiredAutomotiveTypes = listOf(
            "car_repair",
            "car_parts_store", 
            "automotive_repair_shop",
            "car_dealer" // Only if it has service/parts
        )
        
        val hasRequiredType = types.any { type ->
            requiredAutomotiveTypes.contains(type.lowercase())
        }
        
        // Primary automotive keywords that MUST appear in name (not just address)
        val primaryKeywords = listOf(
            "auto repair", "car repair", "automotive repair", "mechanic",
            "auto parts", "car parts", "automotive parts",
            "tuning", "performance", "modification", "custom",
            "auto body", "body shop", "collision",
            "exhaust", "muffler", "transmission", "engine",
            "brake", "alignment", "tire", "wheel"
        )
        
        val hasPrimaryKeyword = primaryKeywords.any { keyword ->
            name.contains(keyword)
        }
        
        // Secondary keywords (can be in name or address, but name is preferred)
        val secondaryKeywords = listOf(
            "garage", "service center", "auto service", "car service",
            "aftermarket", "accessories", "racing", "speed shop"
        )
        
        val hasSecondaryKeyword = secondaryKeywords.any { keyword ->
            name.contains(keyword) || (address.contains(keyword) && hasPrimaryKeyword)
        }
        
        // STRICT exclusion list - these should never appear
        val strictExcludeKeywords = listOf(
            "car rental", "rental car", "car wash", "carwash",
            "gas station", "gas station", "fuel", "petrol",
            "parking", "parking lot", "parking garage",
            "taxi", "uber", "lyft", "rideshare",
            "dealership", "new car", "used car", "sales",
            "showroom", "lot", "inventory"
        )
        
        val shouldStrictlyExclude = strictExcludeKeywords.any { keyword ->
            name.contains(keyword) && !name.contains("parts") && 
            !name.contains("service") && !name.contains("repair") &&
            !name.contains("tuning") && !name.contains("performance")
        }
        
        // Additional check: if it's a car_dealer type, must have service/parts/repair in name
        val isDealerOnly = types.contains("car_dealer") && 
                          !name.contains("service") && 
                          !name.contains("parts") && 
                          !name.contains("repair") &&
                          !name.contains("tuning")
        
        // Must pass: (has required type OR has primary keyword OR has secondary with primary)
        // AND NOT excluded AND NOT dealer-only
        val isRelevant = (hasRequiredType || hasPrimaryKeyword || hasSecondaryKeyword) && 
                        !shouldStrictlyExclude && 
                        !isDealerOnly
        
        return isRelevant
    }
    
    private fun isWithinRadius(
        centerLat: Double,
        centerLng: Double,
        pointLat: Double,
        pointLng: Double,
        radiusMeters: Int
    ): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(centerLat, centerLng, pointLat, pointLng, results)
        return results[0] <= radiusMeters
    }
    
    private val placesApiService: PlacesApiService by lazy {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/place/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        retrofit.create(PlacesApiService::class.java)
    }
    
    override suspend fun getPlaceDetails(placeId: String): AuthResult<Shop?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI,
                Place.Field.RATING,
                Place.Field.LAT_LNG,
                Place.Field.OPENING_HOURS,
                Place.Field.TYPES
            )
            
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            
            val response = placesClient.fetchPlace(request).await()
            val place = response.place
            
            val shop = Shop(
                id = place.id ?: "",
                name = place.name ?: "Unknown",
                address = place.address ?: "",
                phone = place.phoneNumber,
                website = place.websiteUri?.toString(),
                rating = place.rating,
                latitude = place.latLng?.latitude ?: 0.0,
                longitude = place.latLng?.longitude ?: 0.0,
                placeId = place.id ?: "",
                openingHours = place.openingHours?.weekdayText,
                types = place.types?.map { it.name }
            )
            
            AuthResult.Success(shop)
        } catch (e: ApiException) {
            AuthResult.Error("Failed to get place details: ${e.message}", e)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get place details: ${e.message}", e)
        }
    }
    
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    val exception = task.exception ?: Exception("Unknown error")
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}

/**
 * Retrofit interface for Google Places API REST endpoints
 */
interface PlacesApiService {
    @GET("nearbysearch/json")
    suspend fun searchNearby(
        @Query("key") key: String,
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String
    ): PlacesNearbyResponse
    
    @GET("textsearch/json")
    suspend fun searchText(
        @Query("key") key: String,
        @Query("query") query: String,
        @Query("location") location: String,
        @Query("radius") radius: Int
    ): PlacesNearbyResponse
}

@Serializable
data class PlacesNearbyResponse(
    val results: List<PlaceResult>? = null,
    val status: String? = null
)

@Serializable
data class PlaceResult(
    @SerialName("place_id") val placeId: String? = null,
    val name: String? = null,
    val vicinity: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val rating: Double? = null,
    val geometry: PlaceGeometry? = null,
    val types: List<String>? = null
)

@Serializable
data class PlaceGeometry(
    val location: PlaceLocation? = null
)

@Serializable
data class PlaceLocation(
    val lat: Double,
    @SerialName("lng") val lng: Double
)

