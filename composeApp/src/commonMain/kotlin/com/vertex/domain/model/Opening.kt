package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Opening(
    val id: String,
    val type: OpeningType,
    val dimensions: SurfaceDimensions,
    val frameWidth: Double = 0.0
) {
    val area: Double
        get() = dimensions.width * dimensions.height
}

@Serializable
enum class OpeningType {
    DOOR, WINDOW, ARCHWAY
}