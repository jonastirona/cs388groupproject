package com.example.modmycar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Request

/**
 * Reddit-backed external feed source.
 * Step: add Retrofit/OkHttp plumbing (no real network call yet).
 */
class RedditFeedRepository {

    // ---- Public token hook (optional for now) ----
    // We'll set this later after we implement OAuth. Safe to leave null.
    @Volatile private var accessToken: String? = null
    fun setAccessToken(token: String?) { accessToken = token }

    // ---- OkHttp + Retrofit (base URL for OAuth endpoints) ----
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // Maps a RedditListing (DTO) → Page(Post, nextCursor)
    // Safe to call even if fields are missing.
    private fun mapListingToPage(listing: RedditListing?): Page {
        val data = listing?.data
        val items: List<Post> = data?.children.orEmpty()
            .mapNotNull { child ->
                val d = child.data ?: return@mapNotNull null

                // pick best image url: preview.source.url > preview.resolutions[...].url > thumbnail
                val imageUrl: String? = when {
                    d.preview?.images?.firstOrNull()?.source?.url?.isNotBlank() == true ->
                        d.preview.images.first().source?.url
                    d.preview?.images?.firstOrNull()?.resolutions?.isNotEmpty() == true ->
                        d.preview.images.first().resolutions.lastOrNull()?.url
                    !d.thumbnail.isNullOrBlank() && d.thumbnail!!.startsWith("http") ->
                        d.thumbnail
                    else -> null
                }?.replace("&amp;", "&") // Reddit escapes URLs

                val createdSeconds: Long = (d.createdUtc ?: 0.0).toLong()

                // build our Post using the existing helper
                mapRedditLinkToPost(
                    redditId = d.id,
                    title = d.title ?: "(no title)",
                    authorName = d.author,
                    createdUtcSeconds = createdSeconds,
                    imageUrl = imageUrl,
                    permalink = d.permalink ?: "/"
                )
            }

        return Page(items = items, nextCursor = data?.after)
    }

    // Build a search string like: (exhaust OR intake) AND (subreddit:cars OR subreddit:Cartuning)
    private fun buildQuery(userQuery: String, subs: List<String>): String {
        val q = userQuery.trim().ifEmpty { DEFAULT_QUERY }
        val subExpr = subs.takeUnless { it.isEmpty() }?.joinToString(" OR ") { "subreddit:$it" }
        return if (subExpr.isNullOrBlank()) q else "($q) AND ($subExpr)"
    }

    /**
     * Fetches an application-only OAuth token from Reddit and stores it in this repo.
     * TEMP for development. For production, move this to a backend to avoid shipping secrets.
     *
     * @return true if a token was obtained and set.
     */
    suspend fun fetchAppOnlyToken(
        clientId: String,
        clientSecret: String,
        scope: String = "read"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://www.reddit.com/api/v1/access_token")
                .header("Authorization", Credentials.basic(clientId, clientSecret))
                .header("User-Agent", "ModMyCar/1.0 (Android) by u_modmycar_app")
                .post(
                    FormBody.Builder()
                        .add("grant_type", "client_credentials")
                        .add("scope", scope)
                        .build()
                )
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val body = resp.body?.string().orEmpty()
                // Minimal parse without adding dependencies
                // looks like: {"access_token":"...","token_type":"bearer","expires_in":3600,...}
                val token = "\"access_token\":\""
                val start = body.indexOf(token)
                if (start == -1) return@withContext false
                val from = start + token.length
                val end = body.indexOf('"', from)
                if (end <= from) return@withContext false
                val access = body.substring(from, end)
                setAccessToken(access)
                return@withContext access.isNotBlank()
            }
        } catch (_: Throwable) {
            false
        }
    }

    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            // Reddit asks for a descriptive UA
            .header("User-Agent", "ModMyCar/1.0 (Android) by u_modmycar_app")

        // Add Bearer only if we have a token (we'll wire OAuth later)
        accessToken?.let { builder.header("Authorization", "Bearer $it") }

        chain.proceed(builder.build())
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://oauth.reddit.com/") // listing/search live here
            .addConverterFactory(MoshiConverterFactory.create())
            .client(httpClient)
            .build()
    }

    private val api: RedditApi by lazy { retrofit.create(RedditApi::class.java) }

    // ---- Types ----
    data class Page(
        val items: List<Post>,
        val nextCursor: String?
    )

    // ---- Public API (still mocked for now) ----
    suspend fun getPosts(
        query: String,
        subreddits: List<String>,
        sort: String = "hot",
        time: String = "week",
        limit: Int = 25,
        after: String? = null
    ): Page {
        // If we don’t have a token yet, keep returning mock data so the app still shows content.
        val token = accessToken
        if (token.isNullOrBlank()) {
            val mockPosts = listOf(
                mapRedditLinkToPost(
                    redditId = "t3_demo1",
                    title = "My turbo install progress",
                    authorName = "u_speedyboi",
                    createdUtcSeconds = System.currentTimeMillis() / 1000L,
                    imageUrl = "https://i.redd.it/ob1car.jpg",
                    permalink = "/r/Cartuning/comments/demo1"
                ),
                mapRedditLinkToPost(
                    redditId = "t3_demo2",
                    title = "Fresh paint and coilovers done!",
                    authorName = "u_paintshop",
                    createdUtcSeconds = System.currentTimeMillis() / 1000L - 3600,
                    imageUrl = "https://i.redd.it/ob2car.jpg",
                    permalink = "/r/Cartuning/comments/demo2"
                )
            )
            val next = if (after == null) "cursor_demo" else null
            return Page(items = mockPosts, nextCursor = next)
        }

        // Real call path (token present)
        return try {
            val q = buildQuery(query, subreddits)
            val listing = api.search(
                q = q,
                limit = limit.coerceIn(1, 100),
                after = after,
                sort = sort,
                time = if (sort == "top") time else null, // 't' only used with sort=top
                type = "link",
                restrictSr = false
            )
            mapListingToPage(listing)
        } catch (t: Throwable) {
            // Graceful fallback on any network/parse error: return empty page and keep cursor
            Page(items = emptyList(), nextCursor = after)
        }
    }

    // ---- Mapping ----
    internal fun mapRedditLinkToPost(
        redditId: String,
        title: String,
        authorName: String?,
        createdUtcSeconds: Long,
        imageUrl: String?,
        permalink: String // kept for future use
    ): Post {
        val media: List<MediaItem> =
            imageUrl?.takeIf { it.isNotBlank() }
                ?.let { listOf(MediaItem(type = "image", url = it)) }
                ?: emptyList()

        val createdAtIso = epochSecondsToIso8601(createdUtcSeconds)

        return Post(
            id = "reddit:$redditId",
            userId = authorName?.let { "reddit:u/$it" } ?: "reddit:u/unknown",
            carId = null,
            caption = title,
            media = media,
            likesCount = 0,
            commentsCount = 0,
            visibility = "public",
            status = "active",
            createdAt = createdAtIso,
            updatedAt = null
        )
    }

    // ---- Helpers (API 24-safe date formatting) ----
    private fun epochSecondsToIso8601(seconds: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(seconds * 1000L))
    }

    companion object {
        val DEFAULT_SUBS = listOf(
            "cars", "Cartuning", "projectcar", "MechanicAdvice", "Justrolledintotheshop"
        )
        const val DEFAULT_QUERY =
            "exhaust OR intake OR tune OR downpipe OR coilovers OR dyno OR build"
    }
}