package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

interface FriendRepository {
    suspend fun getFriends(userId: String): List<UserProfile>
    suspend fun searchUsers(
        query: String,
        excludeIds: Set<String>
    ): List<UserProfile>

    suspend fun addFriend(requesterId: String, addresseeId: String)
}

class SupabaseFriendRepository(
    private val client: SupabaseClient = com.example.modmycar.SupabaseClient.client
) : FriendRepository {

    override suspend fun getFriends(userId: String): List<UserProfile> {
        if (userId.isBlank()) return emptyList()
        val friendIds = fetchFriendIds(userId)
        return fetchProfiles(friendIds)
    }

    override suspend fun searchUsers(
        query: String,
        excludeIds: Set<String>
    ): List<UserProfile> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val usernameMatches = client.from("profiles").select(columns = Columns.ALL) {
            filter { ilike("username", "%$trimmed%") }
            limit(25)
        }.decodeList<UserProfile>()

        val displayMatches = client.from("profiles").select(columns = Columns.ALL) {
            filter { ilike("display_name", "%$trimmed%") }
            limit(25)
        }.decodeList<UserProfile>()

        return (usernameMatches + displayMatches)
            .distinctBy { it.id }
            .filterNot { excludeIds.contains(it.id) }
    }

    override suspend fun addFriend(requesterId: String, addresseeId: String) {
        if (requesterId == addresseeId) return
        val payload = mapOf(
            "requester_id" to requesterId,
            "addressee_id" to addresseeId,
            "status" to "accepted"
        )
        client.from("friendships").insert(payload)
    }

    private suspend fun fetchFriendIds(userId: String): List<String> {
        val outgoing = client.from("friendships").select {
            filter {
                eq("status", "accepted")
                eq("requester_id", userId)
            }
        }.decodeList<Friendship>()

        val incoming = client.from("friendships").select {
            filter {
                eq("status", "accepted")
                eq("addressee_id", userId)
            }
        }.decodeList<Friendship>()

        return (outgoing.map { it.addresseeId } + incoming.map { it.requesterId })
            .distinct()
    }

    private suspend fun fetchProfiles(ids: List<String>): List<UserProfile> {
        if (ids.isEmpty()) return emptyList()
        val profiles = mutableListOf<UserProfile>()
        for (id in ids) {
            val profile = client.from("profiles").select(columns = Columns.ALL) {
                filter { eq("id", id) }
            }.decodeSingleOrNull<UserProfile>()
            profile?.let { profiles += it }
        }
        return profiles
    }
}

class LocalFriendRepository : FriendRepository {

    private val demoProfiles = listOf(
        UserProfile(id = "demo-1", username = "boosted_ben", display_name = "Boosted Ben"),
        UserProfile(id = "demo-2", username = "turbo_tina", display_name = "Turbo Tina"),
        UserProfile(id = "demo-3", username = "garage_gary", display_name = "Garage Gary")
    )

    override suspend fun getFriends(userId: String): List<UserProfile> = demoProfiles

    override suspend fun searchUsers(
        query: String,
        excludeIds: Set<String>
    ): List<UserProfile> = demoProfiles.filterNot { excludeIds.contains(it.id) }

    override suspend fun addFriend(requesterId: String, addresseeId: String) { /* no-op */ }
}

