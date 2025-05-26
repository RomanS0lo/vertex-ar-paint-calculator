package com.vertex.domain.model

data class PaintEstimate(
    val room: Room,
    val selectedPaint: Paint,
    val surfaceEstimates: List<SurfaceEstimate>,
    val totalEstimate: TotalEstimate,
    val recommendations: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)