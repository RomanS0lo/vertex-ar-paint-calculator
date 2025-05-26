package com.vertex.domain.model

data class SurfaceEstimate(
    val surface: Surface,
    val paint: Paint,
    val coatsNeeded: Int,
    val primerNeeded: Boolean,
    val paintQuantity: PaintQuantity
)
