package com.vertex.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PaintEstimate(
    val room: Room,
    val selectedPaint: Paint,
    val surfaceEstimates: List<SurfaceEstimate>,
    val totalEstimate: TotalEstimate,
    val recommendations: List<String>,
    val createdAt: Instant = Clock.System.now()
)