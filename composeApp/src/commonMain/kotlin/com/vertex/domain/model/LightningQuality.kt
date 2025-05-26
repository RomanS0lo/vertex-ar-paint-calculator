package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LightingQuality {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT
}