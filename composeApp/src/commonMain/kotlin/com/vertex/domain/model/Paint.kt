package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Paint(
    val id: String,
    val brand: String,
    val name: String,
    val color: ColorInfo,
    val type: PaintType,
    val finish: PaintFinish,
    val coverage: CoverageInfo,
    val pricePerGallon: Double
)
@Serializable
enum class PaintType {
    LATEX, OIL_BASED, ACRYLIC, PRIMER
}
@Serializable
enum class PaintFinish {
    FLAT, EGGSHELL, SATIN, SEMI_GLOSS, GLOSS
}
@Serializable
data class CoverageInfo(
    val baseCoatsNeeded: Int = 2,
    val sqFtPerGallon: Int = 350,
    val hideRating: HideRating = HideRating.GOOD
)
@Serializable
enum class HideRating {
    EXCELLENT, GOOD, FAIR
}