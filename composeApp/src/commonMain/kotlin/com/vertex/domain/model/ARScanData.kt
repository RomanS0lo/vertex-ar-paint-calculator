package com.vertex.domain.model

data class ARScanData(
    val planes: List<ARPlane>,
    val roomDimensions: RoomDimensions,
    val confidence: Float
)
