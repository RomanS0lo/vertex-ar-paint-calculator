package com.vertex.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String,
    val name: String,
    val dimensions: RoomDimensions,
    val surfaces: List<Surface>,
    val openings: List<Opening>,
    val createdAt: Instant = Clock.System.now()
) {
    val totalWallArea: Double
        get() = surfaces.filter { it.type == SurfaceType.WALL }
            .sumOf { it.area } - openings.sumOf { it.area }

    val ceilingArea: Double
        get() = dimensions.length * dimensions.width
}
