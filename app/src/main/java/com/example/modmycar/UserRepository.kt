package com.example.modmycar

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

interface UserRepository {
    suspend fun getProfile(userId: String): AuthResult<UserProfile?>
    suspend fun createProfile(profile: UserProfile): AuthResult<UserProfile>
    suspend fun updateProfile(userId: String, username: String?, displayName: String?): AuthResult<UserProfile>
    suspend fun deleteProfile(userId: String): AuthResult<Unit>
}

class SupabaseUserRepository(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : UserRepository {

    override suspend fun getProfile(userId: String): AuthResult<UserProfile?> {
        return try {
            val profile = supabaseClient.from("profiles")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingleOrNull<UserProfile>()
            AuthResult.Success(profile)
        } catch (e: Exception) {
            AuthResult.Error("Failed to get profile: ${e.message}", e)
        }
    }

    override suspend fun createProfile(profile: UserProfile): AuthResult<UserProfile> {
        return try {
            // Use upsert to handle RLS policy - this will insert if not exists, update if exists
            val created = supabaseClient.from("profiles")
                .upsert(profile) {
                    select(Columns.ALL)
                }.decodeSingle<UserProfile>()
            AuthResult.Success(created)
        } catch (e: Exception) {
            // Better error message for RLS policy issues
            val errorMessage = if (e.message?.contains("row-level security") == true) {
                "Unable to create profile. Please ensure your account is verified and try again."
            } else {
                "Failed to create profile: ${e.message}"
            }
            AuthResult.Error(errorMessage, e)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        username: String?,
        displayName: String?
    ): AuthResult<UserProfile> {
        return try {
            // Use upsert instead of update to handle both insert and update cases
            // This ensures the profile exists and matches the user ID
            val profile = UserProfile(
                id = userId,
                username = username,
                display_name = displayName
            )
            
            val updated = supabaseClient.from("profiles")
                .upsert(profile) {
                    select(Columns.ALL)
                }.decodeSingle<UserProfile>()
            AuthResult.Success(updated)
        } catch (e: Exception) {
            val errorMessage = if (e.message?.contains("row-level security") == true) {
                "Unable to update profile. Please ensure your account is verified and try again."
            } else {
                "Failed to update profile: ${e.message}"
            }
            AuthResult.Error(errorMessage, e)
        }
    }

    override suspend fun deleteProfile(userId: String): AuthResult<Unit> {
        return try {
            supabaseClient.from("profiles")
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("Failed to delete profile: ${e.message}", e)
        }
    }
}

