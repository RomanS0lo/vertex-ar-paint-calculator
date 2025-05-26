package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ARScanData(
    val planes: List<ARPlane>,
    val roomDimensions: RoomDimensions,
    val confidence: Float
)
