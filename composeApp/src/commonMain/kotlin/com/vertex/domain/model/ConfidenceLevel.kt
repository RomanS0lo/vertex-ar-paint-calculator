package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConfidenceLevel {
    HIGH, MEDIUM, LOW
}