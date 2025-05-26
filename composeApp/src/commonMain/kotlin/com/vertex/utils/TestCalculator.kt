package com.vertex.utils

import com.vertex.domain.model.ColorInfo
import com.vertex.domain.model.CoverageInfo
import com.vertex.domain.model.Paint
import com.vertex.domain.model.PaintFinish
import com.vertex.domain.model.PaintType
import com.vertex.domain.model.Room
import com.vertex.domain.model.RoomDimensions
import com.vertex.domain.model.Surface
import com.vertex.domain.model.SurfaceCondition
import com.vertex.domain.model.SurfaceDimensions
import com.vertex.domain.model.SurfaceTexture
import com.vertex.domain.model.SurfaceType

fun testPaintCalculator() {
    // Create a sample room
    val room = Room(
        id = "test_room",
        name = "Living Room",
        dimensions = RoomDimensions(4.0, 3.0, 2.7),
        surfaces = listOf(
            Surface(
                id = "wall_1",
                type = SurfaceType.WALL,
                dimensions = SurfaceDimensions(4.0, 2.7),
                texture = SurfaceTexture.SMOOTH,
                currentColor = ColorInfo.fromHex("#FFFFFF"),
                condition = SurfaceCondition.GOOD
            )
        ),
        openings = emptyList()
    )

    // Create sample paint
    val paint = Paint(
        id = "paint_1",
        brand = "Premium Paint Co",
        name = "Classic White",
        color = ColorInfo.fromHex("#FFFFFF"),
        type = PaintType.LATEX,
        finish = PaintFinish.EGGSHELL,
        coverage = CoverageInfo(),
        pricePerGallon = 45.0
    )

    // Calculate estimate
    val calculator = PaintCalculator()
    val estimate = calculator.calculatePaintEstimate(room, paint)

    println("Total gallons needed: ${estimate.totalEstimate.totalPaintGallons}")
    println("Total cost: $${estimate.totalEstimate.totalCost}")
}