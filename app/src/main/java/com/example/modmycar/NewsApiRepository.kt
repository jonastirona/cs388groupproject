package com.example.modmycar

import android.util.Log
import com.example.modmycar.BuildConfig.NEWS_API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.HttpException
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
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
        limit: Int = 20,
        offset: Int = 0,
        userKeywords: List<String>? = null
    ): List<NewsArticleSummary> = withContext(Dispatchers.IO) {
        val apiKey = NEWS_API_KEY
        require(apiKey.isNotBlank()) { "NEWS_API_KEY is missing. Add it to local.properties." }

        val pageSize = limit.coerceIn(1, MAX_PAGE_SIZE)
        val page = (offset / pageSize) + 1

        // Build an interest-aware query when possible, otherwise fall back to the default.
        val effectiveQuery = buildPersonalizedQuery(userKeywords)

        val response = try {
            api.getEverything(
                apiKey = apiKey,
                query = effectiveQuery,
                language = "en",
                sortBy = "publishedAt",
                pageSize = pageSize,
                page = page,
                domains = if (domains.isNotEmpty()) domains.joinToString(",") else null
            )
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            throw IllegalStateException("NewsAPI HTTP ${e.code()}: ${errorBody ?: e.message}")
        }

        if (response.status != "ok") {
            val message = response.message ?: "News API error ${response.code.orEmpty()}"
            throw IllegalStateException(message)
        }

        response.articles.map { it.toSummary() }
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
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
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
