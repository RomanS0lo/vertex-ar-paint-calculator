package com.vertex.domain.model

import kotlinx.serialization.Serializable
@Serializable
data class ARPlane(
    val id: String,
    val type: PlaneType,
    val width: Float,
    val height: Float,
    val area: Float
)

@Serializable
enum class PlaneType {
    HORIZONTAL_UP,    // Floor
    HORIZONTAL_DOWN,  // Ceiling
    VERTICAL,         // Wall
    DOOR,
    WINDOW,
    TABLE,
    SEAT,
    UNKNOWN
}

@Serializable
enum class PlaneOrientation {
    HORIZONTAL,
    VERTICAL,
    ANGLED
}