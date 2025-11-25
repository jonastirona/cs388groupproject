package com.example.modmycar

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
 * Uses annotations instead of direct HTTP calls, with async coroutines.
 */
class NewsApiRepository(
    private val api: NewsApiService = defaultApiService,
    private val query: String = DEFAULT_QUERY,
    private val domains: List<String> = DEFAULT_DOMAINS
) {
    /**
     * Asynchronously fetches articles from NewsAPI.
     * Uses suspend function for coroutine-based async execution.
     */
    suspend fun getArticles(limit: Int = 20, offset: Int = 0): List<NewsArticleSummary> = withContext(Dispatchers.IO) {
        val apiKey = NEWS_API_KEY
        require(apiKey.isNotBlank()) { "NEWS_API_KEY is missing. Add it to local.properties." }

        val pageSize = limit.coerceIn(1, MAX_PAGE_SIZE)
        val page = (offset / pageSize) + 1

        val response = try {
            api.getEverything(
                apiKey = apiKey,
                query = query,
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

    companion object {
        private const val BASE_URL = "https://newsapi.org/v2/"
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_QUERY =
            "(car OR automotive OR tuner OR \"car mod\" OR \"aftermarket\") AND (build OR mod OR tuning OR \"engine swap\" OR \"widebody\")"
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
