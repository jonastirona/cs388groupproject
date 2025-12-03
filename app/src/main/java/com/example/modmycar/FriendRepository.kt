package com.example.modmycar

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

data class UserWithFriendshipStatus(
    val user: UserProfile,
    val friendshipStatus: FriendshipStatus,
    val requestId: String? = null // ID of the friendship/request, if applicable
)

enum class FriendshipStatus {
    NONE,           // No friendship or request
    FRIENDS,        // Already friends
    PENDING_OUTGOING, // Request sent by current user
    PENDING_INCOMING  // Request received by current user
}

interface FriendRepository {
    suspend fun getFriends(userId: String): List<UserProfile>
    suspend fun searchUsers(
        query: String,
        excludeIds: Set<String>,
        currentUserId: String
    ): List<UserWithFriendshipStatus>
    
    suspend fun sendFriendRequest(requesterId: String, addresseeId: String)
    suspend fun getPendingRequests(userId: String): List<Pair<UserProfile, String>> // Returns (UserProfile, requestId)
    suspend fun getSentRequests(userId: String): List<Pair<UserProfile, String>> // Returns (UserProfile, requestId)
    suspend fun acceptFriendRequest(requestId: String, currentUserId: String)
    suspend fun rejectFriendRequest(requestId: String)
    suspend fun cancelFriendRequest(requestId: String)
    suspend fun unfriend(currentUserId: String, friendUserId: String)
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
        excludeIds: Set<String>,
        currentUserId: String
    ): List<UserWithFriendshipStatus> {
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

        val allUsers = (usernameMatches + displayMatches)
            .distinctBy { it.id }
            .filterNot { excludeIds.contains(it.id) }

        // Get friendship status for each user
        val userIds = allUsers.map { it.id }
        val (friendships, requestIds) = getFriendshipsForUsers(currentUserId, userIds)

        return allUsers.map { user ->
            val status = friendships[user.id] ?: FriendshipStatus.NONE
            val requestId = requestIds[user.id]
            UserWithFriendshipStatus(user, status, requestId)
        }
    }

    private suspend fun getFriendshipsForUsers(
        currentUserId: String,
        targetUserIds: List<String>
    ): Pair<Map<String, FriendshipStatus>, Map<String, String>> {
        if (targetUserIds.isEmpty()) return emptyMap<String, FriendshipStatus>() to emptyMap<String, String>()

        // Get all outgoing friendships for current user
        val allOutgoing = client.from("friendships").select {
            filter {
                eq("requester_id", currentUserId)
            }
        }.decodeList<Friendship>()

        // Get all incoming friendships for current user
        val allIncoming = client.from("friendships").select {
            filter {
                eq("addressee_id", currentUserId)
            }
        }.decodeList<Friendship>()

        val statusMap = mutableMapOf<String, FriendshipStatus>()

        // Process outgoing requests/friendships (filter to only target users)
        allOutgoing
            .filter { it.addresseeId in targetUserIds }
            .forEach { friendship ->
                when (friendship.status) {
                    "accepted" -> statusMap[friendship.addresseeId] = FriendshipStatus.FRIENDS
                    "pending" -> statusMap[friendship.addresseeId] = FriendshipStatus.PENDING_OUTGOING
                }
            }

        // Create a map of user ID to request ID for incoming requests
        val requestIdMap = mutableMapOf<String, String>()
        
        // Process incoming requests/friendships (filter to only target users)
        allIncoming
            .filter { it.requesterId in targetUserIds }
            .forEach { friendship ->
                when (friendship.status) {
                    "accepted" -> statusMap[friendship.requesterId] = FriendshipStatus.FRIENDS
                    "pending" -> {
                        statusMap[friendship.requesterId] = FriendshipStatus.PENDING_INCOMING
                        requestIdMap[friendship.requesterId] = friendship.id
                    }
                }
            }
        
        // Process outgoing requests/friendships to get request IDs
        allOutgoing
            .filter { it.addresseeId in targetUserIds && it.status == "pending" }
            .forEach { friendship ->
                requestIdMap[friendship.addresseeId] = friendship.id
            }

        return Pair(statusMap, requestIdMap)
    }

    override suspend fun sendFriendRequest(requesterId: String, addresseeId: String) {
        if (requesterId == addresseeId) return
        
        // Check if request already exists
        val existing = client.from("friendships").select {
            filter {
                eq("requester_id", requesterId)
                eq("addressee_id", addresseeId)
            }
        }.decodeSingleOrNull<Friendship>()
        
        if (existing != null) return // Request already exists
        
        val payload = mapOf(
            "requester_id" to requesterId,
            "addressee_id" to addresseeId,
            "status" to "pending"
        )
        client.from("friendships").insert(payload)
    }

    override suspend fun getPendingRequests(userId: String): List<Pair<UserProfile, String>> {
        val requests = client.from("friendships").select {
            filter {
                eq("addressee_id", userId)
                eq("status", "pending")
            }
        }.decodeList<Friendship>()

        val requesterIds = requests.map { it.requesterId }
        val profiles = fetchProfiles(requesterIds)
        
        // Create a map of profile ID to request ID
        val requestIdMap = requests.associateBy { it.requesterId }
        
        return profiles.map { profile ->
            val requestId = requestIdMap[profile.id]?.id ?: ""
            Pair(profile, requestId)
        }
    }

    override suspend fun getSentRequests(userId: String): List<Pair<UserProfile, String>> {
        val requests = client.from("friendships").select {
            filter {
                eq("requester_id", userId)
                eq("status", "pending")
            }
        }.decodeList<Friendship>()

        val addresseeIds = requests.map { it.addresseeId }
        val profiles = fetchProfiles(addresseeIds)
        
        // Create a map of profile ID to request ID
        val requestIdMap = requests.associateBy { it.addresseeId }
        
        return profiles.map { profile ->
            val requestId = requestIdMap[profile.id]?.id ?: ""
            Pair(profile, requestId)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String, currentUserId: String) {
        try {
            // First check the current state and verify the user is the addressee
            val existing = client.from("friendships").select {
                filter { eq("id", requestId) }
            }.decodeSingleOrNull<Friendship>()
            
            if (existing == null) {
                throw IllegalStateException("Friend request not found with id: $requestId")
            }
            
            if (existing.addresseeId != currentUserId) {
                throw IllegalStateException("You are not authorized to accept this friend request")
            }
            
            if (existing.status != "pending") {
                throw IllegalStateException("Friend request is not pending (status: ${existing.status})")
            }
            
            // Update the status to accepted, ensuring the user is the addressee
            // This helps with RLS policies that check addressee_id
            val result = client.from("friendships").update(mapOf("status" to "accepted")) {
                filter { 
                    eq("id", requestId)
                    eq("addressee_id", currentUserId) // Ensure user is the addressee
                    eq("status", "pending") // Only update if still pending
                }
                select(columns = Columns.ALL)
            }
            
            // Try to decode the result
            val updated = try {
                result.decodeList<Friendship>()
            } catch (e: Exception) {
                // If decode fails, the update might have succeeded but RLS prevents reading
                // Verify by checking the status again
                val verify = client.from("friendships").select {
                    filter { 
                        eq("id", requestId)
                        eq("status", "accepted")
                    }
                }.decodeSingleOrNull<Friendship>()
                
                if (verify != null) {
                    return // Update succeeded
                } else {
                    throw IllegalStateException("Update failed. This may be due to Row Level Security (RLS) policies. Please ensure your Supabase RLS policy allows users to update friendships where they are the addressee. Error: ${e.message}")
                }
            }
            
            if (updated.isEmpty()) {
                throw IllegalStateException("Update failed - no rows were updated. This may be due to Row Level Security (RLS) policies. Please ensure your Supabase RLS policy allows users to update friendships where they are the addressee.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("Failed to accept") == true || e.message?.contains("Row Level Security") == true) {
                throw e // Re-throw our custom exceptions
            }
            throw Exception("Failed to accept friend request: ${e.message}", e)
        }
    }

    override suspend fun rejectFriendRequest(requestId: String) {
        client.from("friendships").delete {
            filter { eq("id", requestId) }
        }
    }

    override suspend fun cancelFriendRequest(requestId: String) {
        client.from("friendships").delete {
            filter { eq("id", requestId) }
        }
    }

    override suspend fun unfriend(currentUserId: String, friendUserId: String) {
        // Delete the friendship record regardless of who is requester or addressee
        // Try both directions since friendship can be stored either way
        val outgoing = client.from("friendships").select {
            filter {
                eq("requester_id", currentUserId)
                eq("addressee_id", friendUserId)
                eq("status", "accepted")
            }
        }.decodeList<Friendship>()

        val incoming = client.from("friendships").select {
            filter {
                eq("requester_id", friendUserId)
                eq("addressee_id", currentUserId)
                eq("status", "accepted")
            }
        }.decodeList<Friendship>()

        val allFriendships = outgoing + incoming

        if (allFriendships.isEmpty()) {
            throw IllegalStateException("Friendship not found")
        }

        // Delete all matching friendships (should only be one, but handle multiple just in case)
        allFriendships.forEach { friendship ->
            client.from("friendships").delete {
                filter { eq("id", friendship.id) }
            }
        }
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
        UserProfile(id = "demo-1", username = "boosted_ben", displayName = "Boosted Ben"),
        UserProfile(id = "demo-2", username = "turbo_tina", displayName = "Turbo Tina"),
        UserProfile(id = "demo-3", username = "garage_gary", displayName = "Garage Gary")
    )

    override suspend fun getFriends(userId: String): List<UserProfile> = demoProfiles

    override suspend fun searchUsers(
        query: String,
        excludeIds: Set<String>,
        currentUserId: String
    ): List<UserWithFriendshipStatus> {
        val users = demoProfiles.filterNot { excludeIds.contains(it.id) }
        return users.map { UserWithFriendshipStatus(it, FriendshipStatus.NONE) }
    }

    override suspend fun sendFriendRequest(requesterId: String, addresseeId: String) { /* no-op */ }
    override suspend fun getPendingRequests(userId: String): List<Pair<UserProfile, String>> = emptyList()
    override suspend fun getSentRequests(userId: String): List<Pair<UserProfile, String>> = emptyList()
    override suspend fun acceptFriendRequest(requestId: String, currentUserId: String) { /* no-op */ }
    override suspend fun rejectFriendRequest(requestId: String) { /* no-op */ }
    override suspend fun cancelFriendRequest(requestId: String) { /* no-op */ }
    override suspend fun unfriend(currentUserId: String, friendUserId: String) { /* no-op */ }
}

