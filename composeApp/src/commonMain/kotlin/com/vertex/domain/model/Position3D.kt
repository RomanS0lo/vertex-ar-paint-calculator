package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Position3D(
    val x: Float,
    val y: Float,
    val z: Float
)
