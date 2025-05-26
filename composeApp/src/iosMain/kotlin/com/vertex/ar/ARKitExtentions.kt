//// iosMain/kotlin/com/vertex/ar/ARKitExtensions.kt
//@file:OptIn(ExperimentalForeignApi::class)
//
//package com.vertex.ar
//
//import platform.ARKit.*
//import kotlinx.cinterop.*
//import platform.Foundation.*
//import kotlinx.datetime.Clock
//import platform.darwin.simd_float4x4
//import kotlin.math.sqrt
//
//// Import your actual data model package - adjust this path to match your structure
//// If your data classes are in a different package, update this import
//import com.vertex.ar.domain.model.*
//import com.vertex.domain.model.LightingQuality
//import com.vertex.domain.model.RoomDimensions
//import com.vertex.domain.model.SurfaceTexture
//
///**
// * Extension functions for ARKit interoperability
// */
//
///**
// * Convert simd_float4x4 transform to position coordinates
// */
//fun simd_float4x4.toPosition(): Triple<Float, Float, Float> {
//    // The transformation matrix has translation in the last column
//    // In Kotlin/Native, we need to access it differently
//    val m30 = this.columns.3.x
//    val m31 = this.columns.3.y
//    val m32 = this.columns.3.z
//
//    return Triple(m30, m31, m32)
//}
//
///**
// * Calculate distance between two AR anchors
// */
//fun ARPlaneAnchor.distanceTo(other: ARPlaneAnchor): Float {
//    val (x1, y1, z1) = this.transform.toPosition()
//    val (x2, y2, z2) = other.transform.toPosition()
//
//    val dx = x2 - x1
//    val dy = y2 - y1
//    val dz = z2 - z1
//
//    return sqrt(dx * dx + dy * dy + dz * dz)
//}
//
///**
// * Check if this plane anchor represents a door or window
// */
//fun ARPlaneAnchor.isPossibleOpening(): Boolean {
//    return when (this.classification) {
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationDoor -> true
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWindow -> true
//        else -> false
//    }
//}
//
///**
// * Get human-readable tracking state reason
// */
//fun ARCamera.trackingStateReasonDescription(): String {
//    return when (this.trackingState) {
//        ARTrackingState.ARTrackingStateNormal -> "Tracking normally"
//        ARTrackingState.ARTrackingStateNotAvailable -> "Tracking not available"
//        ARTrackingState.ARTrackingStateLimited -> {
//            when (this.trackingStateReason) {
//                ARTrackingStateReason.ARTrackingStateReasonInitializing -> "Initializing AR"
//                ARTrackingStateReason.ARTrackingStateReasonExcessiveMotion -> "Moving too fast"
//                ARTrackingStateReason.ARTrackingStateReasonInsufficientFeatures -> "Not enough visual features"
//                ARTrackingStateReason.ARTrackingStateReasonRelocalizing -> "Relocalizing"
//                else -> "Limited tracking"
//            }
//        }
//        else -> "Unknown state"
//    }
//}
//
///**
// * Convert ARPlaneClassification to SurfaceTexture from common model
// */
//fun ARPlaneAnchorPlaneClassification.toSurfaceTexture(): SurfaceTexture {
//    return when (this) {
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWall -> SurfaceTexture.SMOOTH
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationFloor -> SurfaceTexture.CONCRETE
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationCeiling -> SurfaceTexture.SMOOTH
//        ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationTable -> SurfaceTexture.WOOD
//        else -> SurfaceTexture.UNKNOWN
//    }
//}
//
///**
// * Convert ARPlane to Surface from common model
// */
//fun ARPlaneAnchor.toSurface(): Surface {
//    val (x, y, z) = this.transform.toPosition()
//    val width = this.extent.x
//    val height = this.extent.z
//
//    return Surface(
//        id = this.identifier.UUIDString,
//        type = when (this.classification) {
//            ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWall -> SurfaceType.WALL
//            ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationFloor -> SurfaceType.FLOOR
//            ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationCeiling -> SurfaceType.CEILING
//            ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationDoor -> SurfaceType.DOOR
//            ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWindow -> SurfaceType.WINDOW_FRAME
//            else -> SurfaceType.WALL
//        },
//        area = (width * height),
//        perimeter = 2 * (width + height),
//        texture = this.classification.toSurfaceTexture(),
//        currentColor = null, // Will be analyzed separately
//        condition = SurfaceCondition.GOOD, // Default, can be refined
//        orientation = when (this.alignment) {
//            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> SurfaceOrientation.HORIZONTAL
//            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> SurfaceOrientation.VERTICAL
//            else -> SurfaceOrientation.ANGLED
//        },
//        obstacles = emptyList(), // Will be detected separately
//        isPaintable = this.classification != ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWindow,
//        boundingBox = BoundingBox(
//            min = Position3D(x - width / 2, y - height / 2, z),
//            max = Position3D(x + width / 2, y + height / 2, z)
//        )
//    )
//}
//
///**
// * Estimate room dimensions from detected planes
// */
//fun estimateRoomDimensions(planes: List<ARPlaneAnchor>): RoomDimensions {
//    val walls = planes.filter {
//        it.alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical
//    }
//    val floors = planes.filter {
//        it.alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal &&
//                it.transform.toPosition().second < 1.0 // Y position indicates floor
//    }
//
//    // Calculate room dimensions based on wall positions
//    if (walls.size >= 2) {
//        val positions = walls.map { it.transform.toPosition() }
//        val xPositions = positions.map { it.first }
//        val zPositions = positions.map { it.third }
//
//        val width = (xPositions.maxOrNull() ?: 0f) - (xPositions.minOrNull() ?: 0f)
//        val length = (zPositions.maxOrNull() ?: 0f) - (zPositions.minOrNull() ?: 0f)
//
//        // Estimate height from wall extent or default
//        val height = walls.maxOfOrNull {
//            it.extent.y
//        } ?: 2.5f
//
//        return RoomDimensions(
//            length = length,
//            width = width,
//            height = height,
//            floorArea = width * length,
//            volume = width * length * height,
//            shape = if (walls.size == 4) RoomShape.RECTANGULAR else RoomShape.IRREGULAR
//        )
//    }
//
//    // Fallback to floor-based estimation
//    val floorArea = floors.maxByOrNull {
//        it.extent.x * it.extent.z
//    }
//    return if (floorArea != null) {
//        val width = floorArea.extent.x
//        val length = floorArea.extent.z
//        RoomDimensions(
//            length = length,
//            width = width,
//            height = 2.5f, // Default ceiling height
//            floorArea = width * length,
//            volume = width * length * 2.5f,
//            shape = RoomShape.RECTANGULAR
//        )
//    } else {
//        // Default room dimensions
//        RoomDimensions(
//            length = 4.0f,
//            width = 3.0f,
//            height = 2.5f,
//            floorArea = 12.0f,
//            volume = 30.0f,
//            shape = RoomShape.RECTANGULAR
//        )
//    }
//}
//
///**
// * Detect features (doors/windows) and convert to common model
// */
//fun detectFeatures(planes: List<ARPlaneAnchor>): List<DetectedFeature> {
//    return planes
//        .filter { it.isPossibleOpening() }
//        .map { plane ->
//            val (x, y, z) = plane.transform.toPosition()
//            val width = plane.extent.x
//            val height = plane.extent.z
//
//            DetectedFeature(
//                id = plane.identifier.UUIDString,
//                type = when (plane.classification) {
//                    ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationDoor -> FeatureType.DOOR
//                    ARPlaneAnchorPlaneClassification.ARPlaneAnchorPlaneClassificationWindow -> FeatureType.WINDOW
//                    else -> FeatureType.OTHER
//                },
//                area = width * height,
//                position = Position3D(x, y, z),
//                dimensions = Dimensions(
//                    width = width,
//                    height = height
//                ),
//                confidence = 0.9f // ARKit classifications are generally reliable
//            )
//        }
//}
//
///**
// * Convert ARLightEstimate to comprehensive LightingCondition
// */
//fun ARLightEstimate.toLightingCondition(): LightingCondition {
//    val currentHour = Clock.System.now().toEpochMilliseconds() % (24 * 60 * 60 * 1000) / (60 * 60 * 1000)
//
//    return LightingCondition(
//        ambientIntensity = this.ambientIntensity.toFloat(),
//        colorTemperature = this.ambientColorTemperature.toFloat(),
//        lightSources = listOf(), // Would need additional analysis
//        shadowAreas = listOf(), // Would need additional analysis
//        overallQuality = when {
//            ambientIntensity > 1000 -> LightingQuality.EXCELLENT
//            ambientIntensity > 500 -> LightingQuality.GOOD
//            ambientIntensity > 200 -> LightingQuality.FAIR
//            else -> LightingQuality.POOR
//        },
//        timeOfDay = when (currentHour.toInt()) {
//            in 5..8 -> TimeOfDay.EARLY_MORNING
//            in 8..12 -> TimeOfDay.MORNING
//            in 12..17 -> TimeOfDay.AFTERNOON
//            in 17..20 -> TimeOfDay.EVENING
//            else -> TimeOfDay.NIGHT
//        },
//        naturalLightPercentage = if (currentHour in 6..18) 0.7f else 0.1f,
//        artificialLightTypes = listOf(
//            when {
//                ambientColorTemperature < 3500 -> ArtificialLightType.WARM_WHITE
//                ambientColorTemperature < 5000 -> ArtificialLightType.NEUTRAL_WHITE
//                else -> ArtificialLightType.DAYLIGHT
//            }
//        ),
//        colorRenderingIndex = 85f, // Estimate based on color temperature
//        illuminanceMap = null
//    )
//}
//
///**
// * Build complete ArScanResult from ARKit data
// */
//fun buildArScanResult(
//    scanId: String,
//    planes: List<ARPlaneAnchor>,
//    lightEstimate: ARLightEstimate?,
//    trackingState: ARCamera.TrackingState
//): ArScanResult {
//    val surfaces = planes.map { it.toSurface() }
//    val detectedFeatures = detectFeatures(planes)
//    val roomDimensions = estimateRoomDimensions(planes)
//
//    return ArScanResult(
//        scanId = scanId,
//        timestamp = Clock.System.now(),
//        roomDimensions = roomDimensions,
//        surfaces = surfaces,
//        detectedFeatures = detectedFeatures,
//        lightingCondition = lightEstimate?.toLightingCondition() ?: LightingCondition(
//            ambientIntensity = 500f,
//            colorTemperature = 4000f,
//            lightSources = emptyList(),
//            shadowAreas = emptyList(),
//            overallQuality = LightingQuality.FAIR,
//            timeOfDay = TimeOfDay.AFTERNOON,
//            naturalLightPercentage = 0.5f,
//            artificialLightTypes = listOf(ArtificialLightType.NEUTRAL_WHITE),
//            colorRenderingIndex = 80f,
//            illuminanceMap = null
//        ),
//        scanQuality = ScanQuality(
//            overallScore = if (trackingState == ARCamera.TrackingState.ARTrackingStateNormal) 0.9f else 0.5f,
//            completeness = surfaces.size / 10f, // Rough estimate
//            accuracy = 0.02f, // 2cm accuracy with good tracking
//            stability = if (trackingState == ARCamera.TrackingState.ARTrackingStateNormal) 1.0f else 0.5f,
//            warnings = emptyList()
//        ),
//        environmentalFactors = EnvironmentalFactors(
//            temperature = 20f, // Would need external sensor
//            humidity = 50f, // Would need external sensor
//            airflow = AirflowLevel.MODERATE,
//            dustLevel = DustLevel.LOW
//        ),
//        scanMetadata = ScanMetadata(
//            deviceModel = NSProcessInfo.processInfo.deviceModel,
//            hasLidar = hasLiDARSupport(),
//            scanDuration = 0L, // Would be tracked separately
//            pointCloudDensity = 1000,
//            accuracyLevel = if (hasLiDARSupport()) AccuracyLevel.HIGH else AccuracyLevel.MEDIUM,
//            calibrationStatus = CalibrationStatus.CALIBRATED
//        )
//    )
//}
//
///**
// * Check if device supports LiDAR
// */
//fun hasLiDARSupport(): Boolean {
//    return ARWorldTrackingConfiguration.supportsSceneReconstruction(
//        ARSceneReconstruction.ARSceneReconstructionMesh
//    )
//}
//
///**
// * Check if device supports depth sensing
// */
//fun hasDepthSupport(): Boolean {
//    return ARWorldTrackingConfiguration.supportsFrameSemantics(
//        ARFrameSemantics.ARFrameSemanticSceneDepth
//    )
//}
//
///**
// * Create optimized AR configuration based on device capabilities
// */
//fun createOptimizedARConfiguration(): ARWorldTrackingConfiguration {
//    return ARWorldTrackingConfiguration().apply {
//        // Basic configuration
//        planeDetection = ARPlaneDetection.ARPlaneDetectionHorizontal or ARPlaneDetection.ARPlaneDetectionVertical
//        isLightEstimationEnabled = true
//        isAutoFocusEnabled = true
//
//        // Advanced features based on device capabilities
//        if (hasLiDARSupport()) {
//            sceneReconstruction = ARSceneReconstruction.ARSceneReconstructionMesh
//            println("ARKit: LiDAR enabled for enhanced accuracy")
//        }
//
//        if (hasDepthSupport()) {
//            frameSemantics = ARFrameSemantics.ARFrameSemanticSceneDepth
//            println("ARKit: Depth sensing enabled")
//        }
//
//        // Set world alignment
//        worldAlignment = ARConfiguration.WorldAlignment.ARWorldAlignmentGravity
//
//        // Enable collaboration if needed for multi-device scanning
//        isCollaborationEnabled = false
//    }
//}