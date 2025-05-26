package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DetectedPlane(
    val id: String,
    val type: PlaneType,
    val area: Float, // in square meters
    val center: Position3D,
    val orientation: PlaneOrientation
)
