package com.vertex.utils

import com.vertex.domain.model.ColorInfo
import com.vertex.domain.model.HideRating
import kotlin.math.sqrt

class ColorAnalyzer {

    fun calculateColorDifference(currentColor: ColorInfo, targetColor: ColorInfo): Double {
        // Simplified Delta E calculation (CIE76)
        // In production, would use proper LAB color space conversion

        val current = currentColor.rgb
        val target = targetColor.rgb

        val deltaR = (current.red - target.red).toDouble()
        val deltaG = (current.green - target.green).toDouble()
        val deltaB = (current.blue - target.blue).toDouble()

        return sqrt(deltaR * deltaR + deltaG * deltaG + deltaB * deltaB)
    }

    fun analyzeCoverageNeeds(
        currentColor: ColorInfo,
        targetColor: ColorInfo,
        paintQuality: HideRating
    ): CoverageAnalysis {

        val deltaE = calculateColorDifference(currentColor, targetColor)

        val difficulty = when {
            deltaE < 50 -> CoverageDifficulty.EASY
            deltaE < 100 -> CoverageDifficulty.MODERATE
            deltaE < 150 -> CoverageDifficulty.DIFFICULT
            else -> CoverageDifficulty.VERY_DIFFICULT
        }

        return CoverageAnalysis(
            deltaE = deltaE,
            difficulty = difficulty,
            recommendedCoats = calculateRecommendedCoats(deltaE, paintQuality),
            primerRecommended = deltaE > 100
        )
    }

    private fun calculateRecommendedCoats(deltaE: Double, quality: HideRating): Int {
        val baseCoats = when (quality) {
            HideRating.EXCELLENT -> 2
            HideRating.GOOD -> 2
            HideRating.FAIR -> 3
        }

        return when {
            deltaE > 200 -> baseCoats + 2
            deltaE > 100 -> baseCoats + 1
            deltaE < 30 -> maxOf(1, baseCoats - 1)
            else -> baseCoats
        }
    }
}

data class CoverageAnalysis(
    val deltaE: Double,
    val difficulty: CoverageDifficulty,
    val recommendedCoats: Int,
    val primerRecommended: Boolean
)

enum class CoverageDifficulty {
    EASY, MODERATE, DIFFICULT, VERY_DIFFICULT
}