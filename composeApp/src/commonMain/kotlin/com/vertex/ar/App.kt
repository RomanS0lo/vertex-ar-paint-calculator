package com.vertex.ar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.vertex.utils.PaintCalculator
import com.vertex.utils.testPaintCalculator
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import vertex.composeapp.generated.resources.Res
import vertex.composeapp.generated.resources.compose_multiplatform

@Composable
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎨 Vertex Paint Calculator",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Test our calculator
            var result by remember { mutableStateOf<String?>(null) }

            Button(
                onClick = {
                    result = runPaintTest()
                }
            ) {
                Text("Test Calculator")
            }

            result?.let { text ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// Put the test function directly in App.kt
fun runPaintTest(): String {
    return try {
        // Create sample room
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
            brand = "Premium Paint",
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

        // FIX: Define wallArea as local variable
        val wallArea = room.totalWallArea
        val gallons = estimate.totalEstimate.totalPaintGallons
        val cost = estimate.totalEstimate.totalCost

        "✅ Calculator Works!\n\n" +
                "Room: ${room.name}\n" +
                "Wall Area: ${wallArea.toInt()} m²\n" +
                "Paint Needed: ${gallons} gallons\n" +
                "Total Cost: $${cost.toInt()}\n" +
                "Coats: ${estimate.surfaceEstimates.first().coatsNeeded}\n" +
                "Primer: ${if (estimate.surfaceEstimates.first().primerNeeded) "Yes" else "No"}"

    } catch (e: Exception) {
        "❌ Error: ${e.message}"
    }
}