package com.vertex.domain.model

data class ARScanResult(
    val isTracking: Boolean,
    val detectedPlanes: List<ARPlane>,
    val scanProgress: Float,
    val roomArea: Double
)