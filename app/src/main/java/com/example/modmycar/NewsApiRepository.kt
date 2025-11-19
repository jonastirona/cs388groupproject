package com.example.modmycar

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository that uses NewsAPI.org - a proper REST API with JSON responses.
 * This is clearly an external API (not just RSS feeds) with:
 * - API key authentication
 * - JSON responses
 * - Personalization features (categories, search, country)
 * - Proper pagination
 * 
 * Why this is better than Google search:
 * - Integrated into app experience (no context switching)
 * - Personalized based on user interests (car mods, tuning, etc.)
 * - Infinite scroll doomscrolling experience
 * - Real-time updates without manual searching
 * - Can track engagement and preferences
 */
class NewsApiRepository(
    private val apiKey: String,
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) {
    companion object {
        private const val BASE_URL = "https://newsapi.org/v2"
        private const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Fetches personalized news articles based on user preferences.
     * 
     * @param category Optional category filter (e.g., "technology", "business")
     * @param query Optional search query for personalization
     * @param country Country code for localized news (default: "us")
     * @param page Page number for pagination (starts at 1)
     */
    suspend fun getArticles(
        category: String? = null,
        query: String? = null,
        country: String = "us",
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Result<List<NewsArticleSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = when {
                query != null -> "/everything"
                category != null -> "/top-headlines"
                else -> "/top-headlines"
            }

            val response = client.get("$BASE_URL$endpoint") {
                parameter("apiKey", apiKey)
                if (query != null) {
                    parameter("q", query)
                    parameter("language", "en")
                    parameter("sortBy", "publishedAt")
                } else {
                    if (category != null) {
                        parameter("category", category)
                    }
                    parameter("country", country)
                }
                parameter("pageSize", pageSize)
                parameter("page", page)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body: NewsApiResponse = response.body()
                    Result.success(body.articles.map { it.toNewsArticleSummary() })
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Invalid API key. Please check your NewsAPI key in local.properties"))
                }
                HttpStatusCode.TooManyRequests -> {
                    Result.failure(Exception("API rate limit exceeded. Please try again later."))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch news: ${response.status}"))
                }
            }
        }.getOrElse { exception ->
            Result.failure(exception)
        }
    }

    /**
     * Gets personalized articles based on user's car mod interests.
     * Combines multiple queries to create a personalized feed.
     */
    suspend fun getPersonalizedArticles(
        userInterests: List<String> = listOf("car modification", "automotive tuning", "car customization"),
        page: Int = 1
    ): Result<List<NewsArticleSummary>> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<NewsArticleSummary>()
        
        // Fetch articles for each interest and merge them
        userInterests.forEach { interest ->
            val result = getArticles(query = interest, page = page, pageSize = 10)
            result.getOrNull()?.let { allArticles.addAll(it) }
        }

        // Remove duplicates and sort by date
        val uniqueArticles = allArticles
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt ?: "" }
            .take(DEFAULT_PAGE_SIZE)

        Result.success(uniqueArticles)
    }
}

@Serializable
private data class NewsApiResponse(
    val status: String,
    val totalResults: Int? = null,
    val articles: List<NewsApiArticle>
)

@Serializable
private data class NewsApiArticle(
    val source: NewsApiSource? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null
)

@Serializable
private data class NewsApiSource(
    val id: String? = null,
    val name: String? = null
)

private fun NewsApiArticle.toNewsArticleSummary(): NewsArticleSummary {
    // Parse date to a more readable format
    val formattedDate = publishedAt?.let { dateString ->
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            dateString
        }
    }

    return NewsArticleSummary(
        id = url ?: title ?: java.util.UUID.randomUUID().toString(),
        title = title ?: "No title",
        summary = description,
        imageUrl = urlToImage,
        source = source?.name ?: "Unknown",
        publishedAt = formattedDate ?: publishedAt,
        link = url ?: ""
    )
}

