package com.example.modmycar

import android.util.Log
import com.example.modmycar.BuildConfig.NEWS_API_KEY
import com.example.modmycar.BuildConfig.NEWS_API_KEY_1
import com.example.modmycar.BuildConfig.NEWS_API_KEY_2
import com.example.modmycar.BuildConfig.NEWS_API_KEY_3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.HttpException
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import java.util.UUID

/**
 * Fetches automotive articles from NewsAPI /v2/everything endpoint using Retrofit.
 */
class NewsApiRepository(
    private val api: NewsApiService = defaultApiService,
    private val query: String = DEFAULT_QUERY,
    private val domains: List<String> = DEFAULT_DOMAINS
) {
    /**
     * Asynchronously fetches articles from NewsAPI.
     *
     * @param userKeywords Optional list of user interest keywords (e.g., derived from their posts,
     * cars, mods, and liked posts). When provided, these are intersected with our internal keyword
     * bank and, if any matches are found, used to build a more specific NewsAPI query. When
     * no matches are found or this is null/empty, the default query is used.
     */
    suspend fun getArticles(
        limit: Int = MAX_PAGE_SIZE,
        offset: Int = 0,
        userKeywords: List<String>? = null
    ): List<NewsArticleSummary> = withContext(Dispatchers.IO) {
        val apiKeys = getApiKeys()
        require(apiKeys.isNotEmpty()) { "No NEWS_API_KEY found. Add at least NEWS_API_KEY to local.properties." }

        val pageSize = limit.coerceIn(1, MAX_PAGE_SIZE)
        val page = (offset / pageSize) + 1

        // Build an interest-aware query when possible, otherwise fall back to the default.
        val effectiveQuery = buildPersonalizedQuery(userKeywords)
        val domainsParam = if (domains.isNotEmpty()) domains.joinToString(",") else null

        var lastException: Exception? = null
        for ((index, apiKey) in apiKeys.withIndex()) {
            try {
                Log.d(TAG, "=".repeat(80))
                Log.d(TAG, "NewsAPI REQUEST - getArticles() [Key ${index + 1}/${apiKeys.size}]")
                Log.d(TAG, "=".repeat(80))
                Log.d(TAG, "API Key: $apiKey")
                Log.d(TAG, "Base URL: $BASE_URL")
                Log.d(TAG, "Endpoint: everything")
                Log.d(TAG, "Request Parameters:")
                Log.d(TAG, "  - query: $effectiveQuery")
                Log.d(TAG, "  - language: en")
                Log.d(TAG, "  - sortBy: publishedAt")
                Log.d(TAG, "  - pageSize: $pageSize")
                Log.d(TAG, "  - page: $page")
                Log.d(TAG, "  - domains: $domainsParam")
                Log.d(TAG, "  - limit (input): $limit")
                Log.d(TAG, "  - offset (input): $offset")
                Log.d(TAG, "  - userKeywords: $userKeywords")
                
                // Build the full URL for logging
                val fullUrl = buildFullUrl(effectiveQuery, pageSize, page, domainsParam)
                Log.d(TAG, "Full Request URL: $fullUrl")
                Log.d(TAG, "-".repeat(80))

                val response = api.getEverything(
                    apiKey = apiKey,
                    query = effectiveQuery,
                    language = "en",
                    sortBy = "publishedAt",
                    pageSize = pageSize,
                    page = page,
                    domains = domainsParam
                )
                
                // Log the complete response JSON
                Log.d(TAG, "=".repeat(80))
                Log.d(TAG, "NewsAPI RESPONSE - Success")
                Log.d(TAG, "=".repeat(80))
                try {
                    val responseJson = json.encodeToString(response)
                    Log.d(TAG, "Response JSON (full):")
                    Log.d(TAG, responseJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to serialize response to JSON: ${e.message}")
                    Log.d(TAG, "Response object: $response")
                }
                Log.d(TAG, "Response Summary:")
                Log.d(TAG, "  - status: ${response.status}")
                Log.d(TAG, "  - totalResults: ${response.totalResults}")
                Log.d(TAG, "  - articles count: ${response.articles.size}")
                Log.d(TAG, "  - code: ${response.code}")
                Log.d(TAG, "  - message: ${response.message}")
                
                // Log first article details as example
                if (response.articles.isNotEmpty()) {
                    Log.d(TAG, "First Article Example:")
                    val first = response.articles[0]
                    Log.d(TAG, "  - title: ${first.title}")
                    Log.d(TAG, "  - source: ${first.source?.name}")
                    Log.d(TAG, "  - url: ${first.url}")
                    Log.d(TAG, "  - publishedAt: ${first.publishedAt}")
                    try {
                        val articleJson = json.encodeToString(first)
                        Log.d(TAG, "  - JSON: $articleJson")
                    } catch (e: Exception) {
                        Log.e(TAG, "  - Failed to serialize article: ${e.message}")
                    }
                }
                Log.d(TAG, "-".repeat(80))

                if (response.status != "ok") {
                    val message = response.message ?: "News API error ${response.code.orEmpty()}"
                    if (isQuotaError(response.code, message) && index < apiKeys.size - 1) {
                        Log.w(TAG, "Quota exceeded for key ${index + 1}, trying next key...")
                        lastException = IllegalStateException(message)
                        continue
                    }
                    Log.e(TAG, "Response status is not 'ok': status=${response.status}, code=${response.code}, message=$message")
                    throw IllegalStateException(message)
                }

                val summaries = response.articles.map { it.toSummary() }
                Log.d(TAG, "Converted ${summaries.size} articles to NewsArticleSummary")
                Log.d(TAG, "=".repeat(80))
                return@withContext summaries
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val isQuota = e.code() == 429 || (errorBody?.contains("quota", ignoreCase = true) == true)
                
                if (isQuota && index < apiKeys.size - 1) {
                    Log.w(TAG, "Quota exceeded for key ${index + 1} (HTTP ${e.code()}), trying next key...")
                    lastException = e
                    continue
                }
                
                Log.e(TAG, "=".repeat(80))
                Log.e(TAG, "NewsAPI RESPONSE - HTTP Error")
                Log.e(TAG, "=".repeat(80))
                Log.e(TAG, "HTTP Status Code: ${e.code()}")
                Log.e(TAG, "Error Message: ${e.message}")
                Log.e(TAG, "Error Body: $errorBody")
                Log.e(TAG, "Response Headers: ${e.response()?.headers()}")
                Log.e(TAG, "-".repeat(80))
                throw IllegalStateException("NewsAPI HTTP ${e.code()}: ${errorBody ?: e.message}")
            } catch (e: Exception) {
                if (index < apiKeys.size - 1) {
                    Log.w(TAG, "Error with key ${index + 1}, trying next key...", e)
                    lastException = e
                    continue
                }
                Log.e(TAG, "=".repeat(80))
                Log.e(TAG, "NewsAPI RESPONSE - Exception")
                Log.e(TAG, "=".repeat(80))
                Log.e(TAG, "Exception Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception Message: ${e.message}")
                Log.e(TAG, "Stack Trace:", e)
                Log.e(TAG, "-".repeat(80))
                throw e
            }
        }
        
        throw lastException ?: IllegalStateException("All API keys exhausted")
    }
    
    private fun getApiKeys(): List<String> {
        val keys = mutableListOf<String>()
        if (NEWS_API_KEY_1.isNotBlank()) keys.add(NEWS_API_KEY_1)
        if (NEWS_API_KEY_2.isNotBlank()) keys.add(NEWS_API_KEY_2)
        if (NEWS_API_KEY_3.isNotBlank()) keys.add(NEWS_API_KEY_3)
        if (keys.isEmpty() && NEWS_API_KEY.isNotBlank()) keys.add(NEWS_API_KEY)
        return keys
    }
    
    private fun isQuotaError(code: String?, message: String?): Boolean {
        return code == "rateLimited" || 
               message?.contains("quota", ignoreCase = true) == true ||
               message?.contains("rate limit", ignoreCase = true) == true
    }
    
    private fun buildFullUrl(query: String, pageSize: Int, page: Int, domains: String?): String {
        val params = mutableListOf<String>().apply {
            add("q=${java.net.URLEncoder.encode(query, "UTF-8")}")
            add("language=en")
            add("sortBy=publishedAt")
            add("pageSize=$pageSize")
            add("page=$page")
            if (domains != null) {
                add("domains=${java.net.URLEncoder.encode(domains, "UTF-8")}")
            }
        }
        return "$BASE_URL everything?${params.joinToString("&")}"
    }

    private fun NewsApiArticle.toSummary(): NewsArticleSummary {
        val safeUrl = url ?: ""
        return NewsArticleSummary(
            id = "newsapi-" + (safeUrl.ifBlank { UUID.randomUUID().toString() }),
            title = title ?: description ?: "Automotive update",
            summary = description ?: content,
            imageUrl = urlToImage,
            source = source?.name,
            publishedAt = publishedAt,
            link = safeUrl
        )
    }

    /**
     * Builds a personalized NewsAPI query string when possible, falling back to [query] when
     * there are no matching interest keywords.
     *
     * The algorithm is intentionally simple:
     * - Intersect [userKeywords] with [KEYWORD_BANK] in a case-insensitive way.
     * - If we find matches, build a query that searches for articles with ANY of those keywords,
     *   while still maintaining automotive relevance by requiring at least one automotive term.
     * - If not, just use the default [query] this repository was created with.
     */
    private fun buildPersonalizedQuery(userKeywords: List<String>?): String {
        if (userKeywords.isNullOrEmpty()) {
            Log.d(TAG, "buildPersonalizedQuery: No user keywords provided, using default query: $query")
            return query
        }

        Log.d(TAG, "buildPersonalizedQuery: Raw user keywords (${userKeywords.size}): ${userKeywords.take(5).joinToString(", ")}${if (userKeywords.size > 5) "..." else ""}")

        // Normalize everything to lowercase for matching.
        val normalizedBank = KEYWORD_BANK.map { it.lowercase() }.toSet()
        val matched = userKeywords
            .flatMap { raw ->
                // Very small heuristic: split on spaces so \"wide body\" can still hit \"widebody\".
                raw.split(' ', ',', ';')
            }
            .mapNotNull { it.trim().takeIf { token -> token.isNotEmpty() } }
            .map { it.lowercase() }
            .filter { token -> normalizedBank.any { bankEntry -> token.contains(bankEntry) || bankEntry.contains(token) } }
            .distinct()

        if (matched.isEmpty()) {
            Log.d(TAG, "buildPersonalizedQuery: No keywords matched the keyword bank, using default query: $query")
            return query
        }

        Log.d(TAG, "buildPersonalizedQuery: Matched keywords (${matched.size}): ${matched.joinToString(", ")}")

        // Keep the query reasonably small; NewsAPI has URL length limits.
        val limited = matched.take(10)
        val interestsClause = limited.joinToString(separator = " OR ") { "\"$it\"" }

        // Always combine user interests with automotive filter to keep results focused and relevant.
        // This ensures we only get automotive-related articles even when user has many matched keywords.
        val automotiveBase = "(car OR automotive OR tuner OR \"car mod\" OR \"aftermarket\" OR build OR mod OR tuning)"
        val finalQuery = "($interestsClause) AND $automotiveBase"
        
        Log.d(TAG, "buildPersonalizedQuery: Final personalized query: $finalQuery")
        return finalQuery
    }

    companion object {
        private const val TAG = "NewsApiRepository"
        private const val BASE_URL = "https://newsapi.org/v2/"
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_QUERY =
            "(car OR automotive OR tuner OR \"car mod\" OR \"aftermarket\") AND (build OR mod OR tuning OR \"engine swap\" OR \"widebody\")"

        /**
         * Very small bank of modding/automotive interest keywords.
         *
         * The caller should pass in raw user interest strings (e.g., titles, captions, car names,
         * mod names, tags). We then match them against this bank to decide what to send to NewsAPI.
         */
        private val KEYWORD_BANK: List<String> = listOf(
            // Mods / Parts
            "widebody", "lip", "splitter", "spoiler", "wing", "diffuser", "canards",
            "coilovers", "air suspension", "exhaust", "headers", "downpipe",
            "turbo", "supercharger", "intercooler", "intake", "manifold",
            "brakes", "rotors", "calipers", "wheels", "rims", "tires",
            "hood", "fender", "bumper", "side skirt", "skirts",
            // Car makes
            "toyota", "honda", "nissan", "mazda", "subaru", "mitsubishi", "lexus", "acura", "infiniti",
            "ford", "chevrolet", "dodge", "jeep", "ram", "gmc", "cadillac", "lincoln", "buick",
            "bmw", "mercedes", "mercedes-benz", "audi", "porsche", "volkswagen", "vw", "mini",
            "volvo", "jaguar", "land rover", "range rover", "bentley", "rolls-royce", "rolls royce",
            "ferrari", "lamborghini", "mclaren", "maserati", "aston martin", "aston",
            "hyundai", "kia", "genesis",
            // Car models
            "silvia", "s13", "s14", "s15", "supra", "rx-7", "rx7", "skyline", "gtr",
            "civic", "si", "type r", "integra", "nsx", "s2000", "rx-8", "miata", "mx-5",
            "brz", "fr-s", "86", "wrx", "sti", "evo", "lancer", "370z", "350z", "z", "fairlady",
            "g35", "g37", "q50", "q60", "is300", "is350", "rc", "gs", "ls",
            "mustang", "camaro", "challenger", "charger", "corvette", "viper", "hellcat",
            "gt500", "shelby", "z06", "zr1", "c8", "c7", "c6",
            "m3", "m4", "m5", "m2", "m6", "amg", "rs3", "rs4", "rs6", "rs7", "s3", "s4", "s5",
            "911", "cayman", "boxster", "panamera", "macan", "cayenne",
            // Racing types
            "drift", "drifting", "time attack", "track", "autocross", "drag", "racing", "circuit"
        )
        private val DEFAULT_DOMAINS = listOf(
            "speedhunters.com",
            "autoweek.com",
            "carscoops.com",
            "autocar.co.uk",
            "jalopnik.com",
            "motortrend.com",
            "roadandtrack.com",
            "topspeed.com",
            "drivetribe.com",
            "thedrive.com"
        )

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private val defaultApiService: NewsApiService by lazy {
            val contentType = "application/json".toMediaType()
            
            // Create HTTP logging interceptor to log all HTTP traffic
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, "HTTP: $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY // Logs headers, body, etc.
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
            retrofit.create(NewsApiService::class.java)
        }
    }
}

/**
 * Retrofit interface defining the NewsAPI endpoints using annotations.
 */
interface NewsApiService {
    @GET("everything")
    suspend fun getEverything(
        @Header("X-Api-Key") apiKey: String,
        @Query("q") query: String,
        @Query("language") language: String,
        @Query("sortBy") sortBy: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int,
        @Query("domains") domains: String? = null
    ): NewsApiResponse
}

// --- Serialization models ---

@Serializable
data class NewsApiResponse(
    val status: String,
    val totalResults: Int? = null,
    val articles: List<NewsApiArticle> = emptyList(),
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class NewsApiArticle(
    val source: NewsApiSource? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    @SerialName("content")
    val content: String? = null
)

@Serializable
data class NewsApiSource(
    val id: String? = null,
    val name: String? = null
)
