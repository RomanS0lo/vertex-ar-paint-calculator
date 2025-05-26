// iosMain/kotlin/com/vertex/ar/ARKitExtensions.kt
package com.vertex.ar

import platform.ARKit.*
import platform.simd.*
import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * Extension functions for ARKit interoperability
 */

/**
 * Convert simd_float4x4 transform to position coordinates
 */
fun simd_float4x4.toPosition(): Triple<Float, Float, Float> {
    val column = this.columns.3
    return Triple(column.x, column.y, column.z)
}

/**
 * Calculate distance between two AR anchors
 */
fun ARPlaneAnchor.distanceTo(other: ARPlaneAnchor): Float {
    val (x1, y1, z1) = this.transform.toPosition()
    val (x2, y2, z2) = other.transform.toPosition()

    val dx = x2 - x1
    val dy = y2 - y1
    val dz = z2 - z1

    return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
}

/**
 * Check if this plane anchor represents a door or window
 */
fun ARPlaneAnchor.isPossibleOpening(): Boolean {
    return when (this.classification) {
        ARPlaneClassificationDoor -> true
        ARPlaneClassificationWindow -> true
        else -> false
    }
}

/**
 * Get human-readable tracking state reason
 */
fun ARCamera.trackingStateReasonDescription(): String {
    return when (this.trackingStateReason) {
        ARTrackingStateReason.ARTrackingStateReasonNone -> "Tracking normally"
        ARTrackingStateReason.ARTrackingStateReasonInitializing -> "Initializing AR"
        ARTrackingStateReason.ARTrackingStateReasonExcessiveMotion -> "Moving too fast"
        ARTrackingStateReason.ARTrackingStateReasonInsufficientFeatures -> "Not enough visual features"
        ARTrackingStateReason.ARTrackingStateReasonRelocalizing -> "Relocalizing"
        else -> "Unknown reason"
    }
}

/**
 * Convert ARPlaneClassification to SurfaceMaterial
 */
fun ARPlaneClassification.toSurfaceMaterial(): SurfaceMaterial {
    return when (this) {
        ARPlaneClassificationWall -> SurfaceMaterial.DRYWALL
        ARPlaneClassificationFloor -> SurfaceMaterial.CONCRETE
        ARPlaneClassificationCeiling -> SurfaceMaterial.DRYWALL
        ARPlaneClassificationTable -> SurfaceMaterial.WOOD
        else -> SurfaceMaterial.DRYWALL
    }
}

/**
 * Estimate room dimensions from detected planes
 */
fun estimateRoomDimensions(planes: List<ARPlaneAnchor>): RoomDimensions {
    val walls = planes.filter { it.alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical }
    val floors = planes.filter {
        it.alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal &&
                it.transform.toPosition().second < 1.0 // Y position indicates floor
    }

    // Calculate room dimensions based on wall positions
    if (walls.size >= 2) {
        val positions = walls.map { it.transform.toPosition() }
        val xPositions = positions.map { it.first }
        val zPositions = positions.map { it.third }

        val width = (xPositions.maxOrNull() ?: 0f) - (xPositions.minOrNull() ?: 0f)
        val length = (zPositions.maxOrNull() ?: 0f) - (zPositions.minOrNull() ?: 0f)

        // Estimate height from wall extent or default
        val height = walls.maxOfOrNull { it.extent.z } ?: 2.5f

        return RoomDimensions(
            width = width.toDouble().coerceAtLeast(1.0),
            length = length.toDouble().coerceAtLeast(1.0),
            height = height.toDouble().coerceIn(2.0, 4.0)
        )
    }

    // Fallback to floor-based estimation
    val floorArea = floors.maxByOrNull { it.extent.x * it.extent.z }
    return if (floorArea != null) {
        RoomDimensions(
            width = floorArea.extent.x.toDouble(),
            length = floorArea.extent.z.toDouble(),
            height = 2.5 // Default ceiling height
        )
    } else {
        // Default room dimensions
        RoomDimensions(3.0, 4.0, 2.5)
    }
}

/**
 * Detect potential openings (doors/windows) in a wall plane
 */
fun detectOpeningsInWallPlane(wallPlane: ARPlaneAnchor, allPlanes: List<ARPlaneAnchor>): List<Opening> {
    val openings = mutableListOf<Opening>()

    // Check for classified openings
    val nearbyOpenings = allPlanes.filter { plane ->
        plane.isPossibleOpening() && plane.distanceTo(wallPlane) < 0.5f
    }

    nearbyOpenings.forEach { opening ->
        val type = when (opening.classification) {
            ARPlaneClassificationDoor -> OpeningType.DOOR
            ARPlaneClassificationWindow -> OpeningType.WINDOW
            else -> OpeningType.OTHER
        }

        openings.add(Opening(
            id = "opening_${opening.identifier.UUIDString}",
            type = type,
            dimensions = SurfaceDimensions(
                width = opening.extent.x.toDouble(),
                height = opening.extent.z.toDouble()
            ),
            frameWidth = 0.1 // Default frame width
        ))
    }

    return openings
}

/**
 * Analyze lighting conditions for paint color accuracy
 */
fun ARLightEstimate.toLightingCondition(): LightingCondition {
    return LightingCondition(
        ambientIntensity = this.ambientIntensity.toFloat(),
        ambientColorTemperature = this.ambientColorTemperature.toFloat()
    )
}

/**
 * Check if device supports LiDAR
 */
fun hasLiDARSupport(): Boolean {
    return ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstructionMesh)
}

/**
 * Check if device supports depth sensing
 */
fun hasDepthSupport(): Boolean {
    return ARWorldTrackingConfiguration.supportsFrameSemantics(ARFrameSemanticSceneDepth)
}

/**
 * Create optimized AR configuration based on device capabilities
 */
fun createOptimizedARConfiguration(): ARWorldTrackingConfiguration {
    return ARWorldTrackingConfiguration().apply {
        // Basic configuration
        planeDetection = ARPlaneDetectionHorizontal or ARPlaneDetectionVertical
        lightEstimationEnabled = true
        isAutoFocusEnabled = true

        // Advanced features based on device capabilities
        if (hasLiDARSupport()) {
            sceneReconstruction = ARSceneReconstructionMesh
            println("ARKit: LiDAR enabled for enhanced accuracy")
        }

        if (hasDepthSupport()) {
            frameSemantics = ARFrameSemanticSceneDepth
            println("ARKit: Depth sensing enabled")
        }

        // Set world alignment
        worldAlignment = ARWorldAlignmentGravity

        // Enable collaboration if needed for multi-device scanning
        isCollaborationEnabled = false
    }
}

/**
 * Data class for AR device capabilities
 */
data class ARDeviceCapabilities(
    val hasLiDAR: Boolean = hasLiDARSupport(),
    val hasDepthSensing: Boolean = hasDepthSupport(),
    val supportedPlaneDetection: Set<PlaneDetectionType> = setOf(
        PlaneDetectionType.HORIZONTAL,
        PlaneDetectionType.VERTICAL
    )
)

enum class PlaneDetectionType {
    HORIZONTAL, VERTICAL
}