package com.vertex.utils

import com.vertex.domain.model.*
import kotlin.math.*

class PaintCalculator {

    fun calculatePaintEstimate(
        room: Room,
        targetPaint: Paint,
        includeWasteFactor: Boolean = true
    ): PaintEstimate {

        val surfaceEstimates = room.surfaces.map { surface ->
            calculateSurfaceEstimate(surface, targetPaint)
        }

        val totalEstimate = calculateTotalEstimate(surfaceEstimates, includeWasteFactor)
        val recommendations = generateRecommendations(room, surfaceEstimates)

        return PaintEstimate(
            room = room,
            selectedPaint = targetPaint,
            surfaceEstimates = surfaceEstimates,
            totalEstimate = totalEstimate,
            recommendations = recommendations
        )
    }

    private fun calculateSurfaceEstimate(
        surface: Surface,
        paint: Paint
    ): SurfaceEstimate {

        // Determine coats needed based on surface condition and paint quality
        val coatsNeeded = determineCoatsNeeded(surface, paint)

        // Check if primer is needed
        val primerNeeded = determinePrimerRequirement(surface, paint)

        // Calculate paint quantity
        val paintQuantity = calculatePaintQuantity(surface, paint, coatsNeeded)

        return SurfaceEstimate(
            surface = surface,
            paint = paint,
            coatsNeeded = coatsNeeded,
            primerNeeded = primerNeeded,
            paintQuantity = paintQuantity
        )
    }

    private fun determineCoatsNeeded(surface: Surface, paint: Paint): Int {
        var baseCoats = paint.coverage.baseCoatsNeeded

        // Adjust for surface condition
        when (surface.condition) {
            SurfaceCondition.POOR -> baseCoats += 1
            SurfaceCondition.FAIR -> baseCoats += 0
            SurfaceCondition.GOOD -> baseCoats += 0
            SurfaceCondition.EXCELLENT -> baseCoats = maxOf(1, baseCoats - 1)
        }

        // Adjust for paint quality
        when (paint.coverage.hideRating) {
            HideRating.EXCELLENT -> baseCoats = maxOf(1, baseCoats - 1)
            HideRating.GOOD -> baseCoats += 0
            HideRating.FAIR -> baseCoats += 1
        }

        return minOf(4, maxOf(1, baseCoats)) // Cap between 1-4 coats
    }

    private fun determinePrimerRequirement(surface: Surface, paint: Paint): Boolean {
        return when {
            surface.condition == SurfaceCondition.POOR -> true
            surface.material == SurfaceMaterial.WOOD && paint.type == PaintType.LATEX -> true
            surface.material == SurfaceMaterial.METAL -> true
            surface.condition == SurfaceCondition.FAIR -> true
            else -> false
        }
    }

    private fun calculatePaintQuantity(
        surface: Surface,
        paint: Paint,
        coats: Int
    ): PaintQuantity {

        // Get coverage adjusted for surface texture
        val adjustedCoverage = adjustCoverageForTexture(
            paint.coverage.sqFtPerGallon,
            surface.texture
        )

        // Convert to square feet (assuming area is in square meters)
        val areaSqFt = surface.area * 10.764 // m² to ft²

        // Calculate total area to cover
        val totalAreaSqFt = areaSqFt * coats

        // Calculate gallons needed
        val gallonsNeeded = totalAreaSqFt / adjustedCoverage

        return PaintQuantity(
            gallons = gallonsNeeded,
            quarts = (gallonsNeeded * 4).toInt(),
            recommendedPurchase = formatPurchaseRecommendation(gallonsNeeded)
        )
    }

    private fun adjustCoverageForTexture(baseCoverage: Int, texture: SurfaceTexture): Double {
        return when (texture) {
            SurfaceTexture.SMOOTH -> baseCoverage * 1.0
            SurfaceTexture.LIGHT_TEXTURE -> baseCoverage * 0.85
            SurfaceTexture.MEDIUM_TEXTURE -> baseCoverage * 0.7
            SurfaceTexture.HEAVY_TEXTURE -> baseCoverage * 0.6
            SurfaceTexture.BRICK -> baseCoverage * 0.5
            SurfaceTexture.STUCCO -> baseCoverage * 0.4
        }
    }

    private fun calculateTotalEstimate(
        surfaceEstimates: List<SurfaceEstimate>,
        includeWasteFactor: Boolean
    ): TotalEstimate {

        val basePaintGallons = surfaceEstimates.sumOf { it.paintQuantity.gallons }
        val basePrimerGallons = surfaceEstimates.count { it.primerNeeded } * 0.5 // Estimate

        // Apply waste factor (15% for professionals)
        val wasteFactor = if (includeWasteFactor) 0.15 else 0.0

        val totalPaintGallons = basePaintGallons * (1 + wasteFactor)
        val totalPrimerGallons = basePrimerGallons * (1 + wasteFactor)

        val paintCost = totalPaintGallons * surfaceEstimates.first().paint.pricePerGallon
        val primerCost = totalPrimerGallons * (surfaceEstimates.first().paint.pricePerGallon * 0.8)

        return TotalEstimate(
            totalPaintGallons = totalPaintGallons,
            totalPrimerGallons = totalPrimerGallons,
            totalCost = paintCost + primerCost,
            confidenceLevel = ConfidenceLevel.HIGH
        )
    }

    private fun generateRecommendations(
        room: Room,
        surfaceEstimates: List<SurfaceEstimate>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Primer recommendations
        if (surfaceEstimates.any { it.primerNeeded }) {
            recommendations.add("🎨 Primer recommended for best results")
        }

        // Multiple coats warning
        val maxCoats = surfaceEstimates.maxOf { it.coatsNeeded }
        if (maxCoats > 2) {
            recommendations.add("⏰ Plan for $maxCoats coats - allow proper drying time")
        }

        // Surface preparation
        if (surfaceEstimates.any { it.surface.condition == SurfaceCondition.POOR }) {
            recommendations.add("🔧 Surface preparation required")
        }

        // Texture considerations
        val heavyTextures = surfaceEstimates.filter {
            it.surface.texture in listOf(
                SurfaceTexture.HEAVY_TEXTURE,
                SurfaceTexture.BRICK,
                SurfaceTexture.STUCCO
            )
        }
        if (heavyTextures.isNotEmpty()) {
            recommendations.add("🖌️ Use high-quality roller for textured surfaces")
        }

        return recommendations
    }

    private fun formatPurchaseRecommendation(gallons: Double): String {
        val wholeGallons = gallons.toInt()
        val remainingQuarts = ((gallons - wholeGallons) * 4).toInt()

        return when {
            wholeGallons == 0 && remainingQuarts > 0 -> "$remainingQuarts quart${if (remainingQuarts > 1) "s" else ""}"
            remainingQuarts == 0 -> "$wholeGallons gallon${if (wholeGallons > 1) "s" else ""}"
            else -> "$wholeGallons gallon${if (wholeGallons > 1) "s" else ""}, $remainingQuarts quart${if (remainingQuarts > 1) "s" else ""}"
        }
    }
}
