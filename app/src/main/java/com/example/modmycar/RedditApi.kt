package com.example.modmycar

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Reddit OAuth API (Listing + Search).
 * We’ll pass the Bearer token via an OkHttp interceptor later.
 *
 * Base URL to use with Retrofit:
 *   https://oauth.reddit.com/
 */
interface RedditApi {

    /**
     * Search across Reddit (we’ll restrict by whitelisted subs in the server later
     * or via query like "subreddit:cars OR subreddit:Cartuning ...").
     *
     * Common params:
     *  - q: query string (e.g., "exhaust OR intake OR coilovers")
     *  - limit: 1..100
     *  - after: listing cursor for pagination (e.g., "t3_abc123")
     *  - sort: "relevance" | "hot" | "new" | "top" | "comments"
     *  - t: when sort == "top", one of "hour","day","week","month","year","all"
     */
    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("limit") limit: Int = 25,
        @Query("after") after: String? = null,
        @Query("sort") sort: String? = null,
        @Query("t") time: String? = null,
        @Query("type") type: String = "link",
        @Query("restrict_sr") restrictSr: Boolean = false
    ): RedditListing
}

/* -------------------- DTOs (Moshi) -------------------- */

@JsonClass(generateAdapter = true)
data class RedditListing(
    @Json(name = "data") val data: RedditListingData?
)

@JsonClass(generateAdapter = true)
data class RedditListingData(
    @Json(name = "children") val children: List<RedditChild> = emptyList(),
    @Json(name = "after") val after: String? = null,
    @Json(name = "before") val before: String? = null
)

@JsonClass(generateAdapter = true)
data class RedditChild(
    @Json(name = "kind") val kind: String? = null, // usually "t3" for posts
    @Json(name = "data") val data: RedditPostData? = null
)

@JsonClass(generateAdapter = true)
data class RedditPostData(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "created_utc") val createdUtc: Double? = null, // seconds (float)
    @Json(name = "permalink") val permalink: String? = null,

    // Thumbnails / preview (prefer preview.images[0].source.url if present)
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "preview") val preview: RedditPreview? = null,

    // Useful for filtering / diagnostics
    @Json(name = "subreddit") val subreddit: String? = null,
    @Json(name = "score") val score: Int? = null,
)

@JsonClass(generateAdapter = true)
data class RedditPreview(
    @Json(name = "images") val images: List<RedditImage> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RedditImage(
    @Json(name = "source") val source: RedditImageSource? = null,
    @Json(name = "resolutions") val resolutions: List<RedditImageSource> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RedditImageSource(
    @Json(name = "url") val url: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)
