package com.example.modmycar

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.regex.Pattern

/**
 * Simple RSS repository that fetches items from a few car-related feeds and
 * normalizes them into the existing Post model so they render in our feed.
 */
class RssFeedRepository(
    private val parser: RssParser = RssParser()
) {

    // Add/remove feeds freely. Keep 2–4 to start (prof suggestion: small, focused).
    private val feeds = listOf(
        "https://www.speedhunters.com/feed/",
        "https://www.autoblog.com/rss.xml",
        "https://www.thedrive.com/feeds/rss" // replace any that 4xx with a known-good feed
    )

    /**
     * Returns a list of Posts mapped from RSS items (merged across feeds),
     * sorted by pubDate descending, and sliced by (offset, limit).
     */
    suspend fun getRssPosts(limit: Int = 20, offset: Int = 0): List<Post> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<RssItem>()

        // Fetch each feed sequentially (keep it simple first).
        for (url in feeds) {
            runCatching {
                val channel = parser.getRssChannel(url)
                allItems += channel.items
            }.onFailure {
            }
        }

        val mapped = allItems
            .asSequence()
            .map { it.toPost() }
            .sortedByDescending { it.createdAt }
            .toList()

        val from = offset.coerceAtLeast(0).coerceAtMost(mapped.size)
        val to = (from + limit).coerceAtMost(mapped.size)
        mapped.subList(from, to)
    }

    // --- Helpers ---

    private fun RssItem.toPost(): Post {
        val imageUrl = imageFrom(this)

        return Post(
            id = "rss-" + (link ?: UUID.randomUUID().toString()),
            userId = "rss",                  // synthetic author
            carId = null,
            caption = title ?: description ?: "RSS item",
            media = imageUrl?.let {
                listOf(
                    MediaItem(
                        type = "image",
                        url = it,
                        width = null,
                        height = null
                    )
                )
            } ?: emptyList(),
            likesCount = 0,
            commentsCount = 0,
            visibility = "public",
            status = "active",
            createdAt = (pubDate ?: "1970-01-01T00:00:00Z"),
            updatedAt = null
        )
    }

    // Try multiple sources: <enclosure>, <media:content>, item.image, then HTML fallbacks
    private fun imageFrom(item: RssItem): String? {
        // Direct image field when provided by the feed
        item.image?.takeIf { it.isNotBlank() }?.let { return it }

        // Fallbacks: look for <img ...> in content or description HTML
        extractFirstImg(item.content.orEmpty())?.let { return it }
        extractFirstImg(item.description.orEmpty())?.let { return it }

        return null
    }

    // Extract first plausible image URL from HTML (handles src, data-src, srcset, style=url(...))
    private fun extractFirstImg(html: String): String? {
        // src / data-src
        val srcPattern = Pattern.compile(
            """<img[^>]+(?:src|data-src)\s*=\s*['"]([^'"]+)['"]""",
            Pattern.CASE_INSENSITIVE
        )
        srcPattern.matcher(html).apply { if (find()) return group(1) }

        // srcset (take the first URL)
        val srcsetPattern = Pattern.compile(
            """<img[^>]+srcset\s*=\s*['"]([^'"]+)['"]""",
            Pattern.CASE_INSENSITIVE
        )
        srcsetPattern.matcher(html).apply {
            if (find()) {
                val firstUrl = group(1)
                    .split(",")
                    .firstOrNull()
                    ?.trim()
                    ?.split(" ")
                    ?.firstOrNull()
                if (!firstUrl.isNullOrBlank()) return firstUrl
            }
        }

        // style="background-image:url(...)"
        val styleUrlPattern = Pattern.compile(
            """background-image\s*:\s*url\(([^)]+)\)""",
            Pattern.CASE_INSENSITIVE
        )
        styleUrlPattern.matcher(html).apply {
            if (find()) {
                return group(1).trim().trim('"', '\'')
            }
        }

        return null
    }
}