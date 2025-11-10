package com.example.modmycar

interface PostRepository {
    suspend fun getFeed(limit: Int = 20, offset: Int = 0): List<Post>
}

class LocalPostRepository : PostRepository {

    // Read from the local fake source for now
    // Will be swapped later to real network implementation
    override suspend fun getFeed(limit: Int, offset: Int): List<Post> {
        // ISO-8601 strings sort correctly; newest first
        val all = LocalPostDataSource
            .getSamplePosts()
            .sortedByDescending { it.createdAt }

        val from = offset.coerceAtLeast(0).coerceAtMost(all.size)
        val to = (from + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }
}
