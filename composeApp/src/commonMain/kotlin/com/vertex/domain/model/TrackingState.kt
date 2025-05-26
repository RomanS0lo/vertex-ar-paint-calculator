package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrackingState {
    NOT_AVAILABLE,
    LIMITED,
    NORMAL
}
