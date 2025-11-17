package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a modification available for a specific car model.
 * Mods are organized in a tree structure where parent mods unlock child mods.
 * This table is preloaded with available mods.
 */
@Serializable
data class Mod(
    val id: String,
    
    @SerialName("car_id")
    val carId: String, // References cars table
    
    @SerialName("parent_id")
    val parentId: String? = null, // References parent mod id (null for root mods)
    
    val name: String,
    val description: String? = null,
    val category: String? = null // e.g., "engine", "exterior", "interior", "suspension"
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

