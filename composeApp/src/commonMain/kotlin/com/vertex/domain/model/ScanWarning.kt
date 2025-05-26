package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScanWarning(
    val type: ScanWarningType,
    val message: String
)

@Serializable
enum class ScanWarningType {
    INSUFFICIENT_LIGHT,
    EXCESSIVE_MOTION,
    INSUFFICIENT_FEATURES,
    TRACKING_LOST,
    INCOMPLETE_COVERAGE
}