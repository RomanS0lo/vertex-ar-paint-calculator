package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Surface(
    val id: String,
    val type: SurfaceType,
    val dimensions: SurfaceDimensions,
    val texture: SurfaceTexture,
    val currentColor: ColorInfo?,
    val condition: SurfaceCondition,
    val material: SurfaceMaterial = SurfaceMaterial.DRYWALL
) {
    val area: Double
        get() = dimensions.width * dimensions.height
}

@Serializable
enum class SurfaceType {
    WALL, CEILING, TRIM
}

@Serializable
enum class SurfaceTexture {
    SMOOTH,           // 350-400 sq ft per gallon
    LIGHT_TEXTURE,    // 300-350 sq ft per gallon
    MEDIUM_TEXTURE,   // 250-300 sq ft per gallon
    HEAVY_TEXTURE,    // 200-250 sq ft per gallon
    BRICK,            // 150-200 sq ft per gallon
    STUCCO           // 100-150 sq ft per gallon
}

@Serializable
enum class SurfaceCondition {
    EXCELLENT,  // No primer needed
    GOOD,       // Light primer recommended
    FAIR,       // Primer required
    POOR        // Heavy primer + prep required
}

@Serializable
enum class SurfaceMaterial {
    DRYWALL, WOOD, METAL, CONCRETE, BRICK, PLASTER
}

@Serializable
data class SurfaceDimensions(
    val width: Double,
    val height: Double
)