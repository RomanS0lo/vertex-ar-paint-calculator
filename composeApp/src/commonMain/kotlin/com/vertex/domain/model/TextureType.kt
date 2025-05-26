package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextureType {
    SMOOTH,
    SEMI_SMOOTH,
    TEXTURED,
    HEAVILY_TEXTURED
}