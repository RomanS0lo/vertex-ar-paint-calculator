package com.vertex.domain.model

data class Opening(
    val id: String,
    val type: OpeningType,
    val dimensions: SurfaceDimensions,
    val frameWidth: Double = 0.0
) {
    val area: Double
        get() = dimensions.width * dimensions.height
}

enum class OpeningType {
    DOOR, WINDOW, ARCHWAY
}