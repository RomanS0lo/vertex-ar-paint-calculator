package com.vertex.domain.model

data class TotalEstimate(
    val totalPaintGallons: Double,
    val totalPrimerGallons: Double,
    val totalCost: Double,
    val confidenceLevel: ConfidenceLevel
)
