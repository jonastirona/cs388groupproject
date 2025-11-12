package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a modification that can be applied to cars.
 * Supports tree structure where mods can unlock other mods.
 */
@Serializable
data class Mod(
    val id: String,
    
    val name: String,
    val description: String? = null,
    
    @SerialName("parent_mod_id")
    val parentModId: String? = null, // Null for root mods, references another mod for child mods
    
    val category: String? = null, // e.g., "engine", "exterior", "interior", "suspension"
    
    @SerialName("image_url")
    val imageUrl: String? = null, // Reference image from storage
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Represents a mod with its tree structure (children mods).
 * Useful for displaying mod trees in the UI.
 */
@Serializable
data class ModWithChildren(
    val mod: Mod,
    val children: List<ModWithChildren> = emptyList()
)

/**
 * Data class for creating a new mod.
 */
@Serializable
data class ModCreate(
    val name: String,
    val description: String? = null,
    
    @SerialName("parent_mod_id")
    val parentModId: String? = null,
    
    val category: String? = null,
    
    @SerialName("image_url")
    val imageUrl: String? = null
)

/**
 * Data class for updating a mod.
 */
@Serializable
data class ModUpdate(
    val name: String? = null,
    val description: String? = null,
    
    @SerialName("parent_mod_id")
    val parentModId: String? = null,
    
    val category: String? = null,
    
    @SerialName("image_url")
    val imageUrl: String? = null
)

