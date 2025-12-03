package com.example.modmycar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceItem(
    val id: String,
    
    @SerialName("garage_car_id")
    val garageCarId: String,
    
    val type: String,
    
    @SerialName("last_service_date")
    val lastServiceDate: String? = null,
    
    @SerialName("interval_days")
    val intervalDays: Int,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class MaintenanceItemCreate(
    @SerialName("garage_car_id")
    val garageCarId: String,
    
    val type: String,
    
    @SerialName("last_service_date")
    val lastServiceDate: String? = null,
    
    @SerialName("interval_days")
    val intervalDays: Int
)

@Serializable
data class MaintenanceItemUpdate(
    @SerialName("last_service_date")
    val lastServiceDate: String? = null
)


enum class MaintenanceType(
    val typeName: String,
    val displayName: String,
    val defaultIntervalDays: Int
) {
    OIL_CHANGE("oil_change", "Oil Change", 180),           // 6 months
    BRAKE_PADS("brake_pads", "Brake Pads", 365),           // 12 months
    TIRE_ROTATION("tire_rotation", "Tire Rotation", 180),  // 6 months
    WASHER_FLUID("washer_fluid", "Windshield Washer Fluid", 90) // 3 months
}

enum class MaintenanceStatus {
    OK,        // More than 14 days until due
    DUE_SOON,  // Within 14 days of due date
    OVERDUE    // Past due date
}

