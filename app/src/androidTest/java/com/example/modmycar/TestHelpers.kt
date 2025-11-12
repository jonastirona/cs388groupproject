package com.example.modmycar

import java.util.UUID

object TestHelpers {
    fun createTestCar(
        id: String = UUID.randomUUID().toString(),
        userId: String = "test-user-id",
        make: String = "Toyota",
        model: String = "Camry",
        color: String = "Blue",
        year: Int = 2020,
        imageUrl: String? = null
    ): Car {
        return Car(
            id = id,
            userId = userId,
            make = make,
            model = model,
            color = color,
            year = year,
            imageUrl = imageUrl,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null
        )
    }

    fun createTestCarCreate(
        userId: String = "test-user-id",
        make: String = "Toyota",
        model: String = "Camry",
        color: String = "Blue",
        year: Int = 2020
    ): CarCreate {
        return CarCreate(
            userId = userId,
            make = make,
            model = model,
            color = color,
            year = year
        )
    }

    fun createTestMod(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Mod",
        description: String? = "Test description",
        parentModId: String? = null,
        category: String? = "engine"
    ): Mod {
        return Mod(
            id = id,
            name = name,
            description = description,
            parentModId = parentModId,
            category = category,
            imageUrl = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null
        )
    }

    fun createTestModCreate(
        name: String = "Test Mod",
        description: String? = "Test description",
        parentModId: String? = null,
        category: String? = "engine"
    ): ModCreate {
        return ModCreate(
            name = name,
            description = description,
            parentModId = parentModId,
            category = category
        )
    }

    fun createTestCarMod(
        id: String = UUID.randomUUID().toString(),
        carId: String = "test-car-id",
        modId: String = "test-mod-id",
        completedAt: String? = null,
        notes: String? = null
    ): CarMod {
        return CarMod(
            id = id,
            carId = carId,
            modId = modId,
            completedAt = completedAt,
            notes = notes,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null
        )
    }
}

