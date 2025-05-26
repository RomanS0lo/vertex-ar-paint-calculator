package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ColorInfo(
    val rgb: RGBColor,
    val hex: String,
    val name: String? = null
) {
    companion object {
        fun fromHex(hex: String): ColorInfo {
            val rgb = RGBColor.fromHex(hex)
            return ColorInfo(rgb = rgb, hex = hex)
        }
    }
}
