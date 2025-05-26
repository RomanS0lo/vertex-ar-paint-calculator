package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TotalEstimate(
    val totalPaintGallons: Double,
    val totalPrimerGallons: Double,
    val totalCost: Double,
    val confidenceLevel: ConfidenceLevel
)
