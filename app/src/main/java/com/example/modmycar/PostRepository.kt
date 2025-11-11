package com.example.modmycar

interface PostRepository {
    // Feed
    suspend fun getFeed(limit: Int = 20, offset: Int = 0): List<Post>

    // CRUD
    suspend fun createPost(post: Post): Post
    suspend fun getPost(postId: String): Post
    suspend fun updatePost(postId: String, post: Post): Post
    suspend fun deletePost(postId: String)
}

/**
 * Temporary local implementation for UI testing.
 * CRUD will be implemented in SupabasePostRepository.
 */
class LocalPostRepository : PostRepository {

    override suspend fun getFeed(limit: Int, offset: Int): List<Post> {
        val all = LocalPostDataSource
            .getSamplePosts()
            .sortedByDescending { it.createdAt }

        val from = offset.coerceAtLeast(0).coerceAtMost(all.size)
        val to = (from + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }

    override suspend fun createPost(post: Post): Post {
        throw UnsupportedOperationException("LocalPostRepository: createPost not supported (use SupabasePostRepository)")
    }

    override suspend fun getPost(postId: String): Post {
        throw UnsupportedOperationException("LocalPostRepository: getPost not supported (use SupabasePostRepository)")
    }

    override suspend fun updatePost(postId: String, post: Post): Post {
        throw UnsupportedOperationException("LocalPostRepository: updatePost not supported (use SupabasePostRepository)")
    }

    override suspend fun deletePost(postId: String) {
        throw UnsupportedOperationException("LocalPostRepository: deletePost not supported (use SupabasePostRepository)")
    }
}