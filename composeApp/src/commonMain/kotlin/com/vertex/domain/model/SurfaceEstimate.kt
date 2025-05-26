package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SurfaceEstimate(
    val surface: Surface,
    val paint: Paint,
    val coatsNeeded: Int,
    val primerNeeded: Boolean,
    val paintQuantity: PaintQuantity
)
