package com.vertex.domain.model


import kotlinx.serialization.Serializable

@Serializable
data class ARScanResult(
    val scanProgress: Float = 0f, // 0.0 to 1.0 representing scan completion
    val detectedPlanes: List<DetectedPlane> = emptyList(),
    val roomArea: Float = 0f, // in square meters
    val lightingCondition: LightingConditionData? = null,
    val trackingState: TrackingState = TrackingState.NOT_AVAILABLE,
    val scanQuality: Float = 0f, // 0.0 to 1.0
    val warnings: List<ScanWarning> = emptyList()
)