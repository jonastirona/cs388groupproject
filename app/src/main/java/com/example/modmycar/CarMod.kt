package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Junction table entry representing a mod applied to a car.
 * Tracks which mods have been completed for each car.
 */
@Serializable
data class CarMod(
    val id: String,
    
    @SerialName("car_id")
    val carId: String,
    
    @SerialName("mod_id")
    val modId: String,
    
    @SerialName("completed_at")
    val completedAt: String? = null, // Null if not completed, timestamp if completed
    
    val notes: String? = null, // Optional notes about the mod for this specific car
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Represents a mod with its completion status for a specific car.
 */
@Serializable
data class ModWithStatus(
    val mod: Mod,
    val carMod: CarMod? = null, // Null if mod not started, contains completion info if started/completed
    val isCompleted: Boolean = false,
    val isUnlocked: Boolean = false // Whether this mod is unlocked (parent mods completed)
)

/**
 * Represents a mod with status and its tree structure (children mods).
 * Useful for displaying mod trees with completion status in the UI.
 */
@Serializable
data class ModWithStatusTree(
    val mod: Mod,
    val carMod: CarMod? = null,
    val isCompleted: Boolean = false,
    val isUnlocked: Boolean = false,
    val children: List<ModWithStatusTree> = emptyList()
)

/**
 * Data class for marking a mod as completed for a car.
 */
@Serializable
data class CarModCreate(
    @SerialName("car_id")
    val carId: String,
    
    @SerialName("mod_id")
    val modId: String,
    
    val notes: String? = null
)

/**
 * Data class for updating a car mod (e.g., marking as completed or updating notes).
 */
@Serializable
data class CarModUpdate(
    @SerialName("completed_at")
    val completedAt: String? = null, // Set to timestamp to mark as completed, null to unmark
    
    val notes: String? = null
)

