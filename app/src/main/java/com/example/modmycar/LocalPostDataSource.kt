package com.example.modmycar

object LocalPostDataSource {

    fun getSamplePosts(): List<Post> {
        val base = listOf(
            Post(
                id = "1",
                userId = "user123",
                carId = "carA",
                caption = "First mod: new exhaust system!",
                media = listOf(MediaItem(type = "image", url = "https://example.com/sample1.jpg", width = 1080, height = 720)),
                likesCount = 12,
                commentsCount = 3,
                createdAt = "2025-11-10T12:00:00Z"
            ),
            Post(
                id = "2",
                userId = "user456",
                caption = "Engine bay update 🔧",
                media = listOf(MediaItem(type = "audio", url = "https://example.com/exhaust.mp3", durationSec = 7.2)),
                likesCount = 5,
                commentsCount = 1,
                createdAt = "2025-11-09T15:30:00Z"
            )
        )

        // Generate more fake posts to test pagination
        val more = (3..50).map { i ->
            Post(
                id = i.toString(),
                userId = "user$i",
                caption = "Mock post #$i",
                media = listOf(MediaItem(type = "image", url = "https://example.com/img$i.jpg", width = 800, height = 600)),
                likesCount = i * 2,
                commentsCount = i % 5,
                createdAt = "2025-11-${(10 - (i % 9)).coerceAtLeast(1)}T10:00:00Z"
            )
        }

        return base + more
    }
}