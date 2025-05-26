package com.vertex.domain.model

data class Room(
    val id: String,
    val name: String,
    val dimensions: RoomDimensions,
    val surfaces: List<Surface>,
    val openings: List<Opening>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalWallArea: Double
        get() = surfaces.filter { it.type == SurfaceType.WALL }
            .sumOf { it.area } - openings.sumOf { it.area }

    val ceilingArea: Double
        get() = dimensions.length * dimensions.width
}
