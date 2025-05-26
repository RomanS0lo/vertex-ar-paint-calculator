package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PaintQuantity(
    val gallons: Double,
    val quarts: Int,
    val recommendedPurchase: String
)
