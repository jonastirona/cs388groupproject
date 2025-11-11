package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order


/**
 * Supabase-backed implementation of the PostRepository.
 * Uses offset/limit pagination against the `posts` table.
 */
class SupabasePostRepository(
    private val client: SupabaseClient
) : PostRepository {

    // ---------------- Feed ----------------
    // Can be switched if/when we go with the Supabase Kotlin version
    override suspend fun getFeed(limit: Int, offset: Int): List<Post> {
        val response = client.from("posts")
            .select {
                filter {
                    eq("status", "active")
                    eq("visibility", "public")
                }
                order("created_at", Order.DESCENDING)
            }

        val all = response.decodeList<Post>()
        val from = offset.coerceAtLeast(0).coerceAtMost(all.size)
        val to = (from + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }

    // ---------------- CRUD ----------------
    override suspend fun createPost(post: Post): Post {
        val inserted = client.from("posts").insert(post) {
            select()
        }
        return inserted.decodeSingle<Post>()
    }

    override suspend fun getPost(postId: String): Post {
        val res = client.from("posts").select {
            filter { eq("id", postId) }
            single()
        }
        return res.decodeSingle<Post>()
    }

    override suspend fun updatePost(postId: String, post: Post): Post {
        val res = client.from("posts").update(post) {
            filter { eq("id", postId) }
            select()
        }
        return res.decodeSingle<Post>()
    }

    override suspend fun deletePost(postId: String) {
        client.from("posts").delete {
            filter { eq("id", postId) }
        }
    }
}