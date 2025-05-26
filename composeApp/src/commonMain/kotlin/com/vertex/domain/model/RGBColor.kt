package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RGBColor(
    val red: Int,   // 0-255
    val green: Int, // 0-255
    val blue: Int   // 0-255
) {
    companion object {
        fun fromHex(hex: String): RGBColor {
            val cleanHex = hex.removePrefix("#")
            return RGBColor(
                red = cleanHex.substring(0, 2).toInt(16),
                green = cleanHex.substring(2, 4).toInt(16),
                blue = cleanHex.substring(4, 6).toInt(16)
            )
        }
    }
}