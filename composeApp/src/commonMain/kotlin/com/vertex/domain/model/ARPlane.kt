package com.vertex.domain.model

data class ARPlane(
    val id: String,
    val type: PlaneType,
    val width: Float,
    val height: Float,
    val area: Float
)

enum class PlaneType {
    WALL, FLOOR, CEILING
}