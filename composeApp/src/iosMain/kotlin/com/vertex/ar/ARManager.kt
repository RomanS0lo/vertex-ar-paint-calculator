// iosMain/kotlin/com/vertex/ar/ARManager.kt
@file:OptIn(ExperimentalForeignApi::class)


package com.vertex.ar

import com.vertex.ar.domain.model.*
import com.vertex.domain.model.ARPlane
import com.vertex.domain.model.ARScanData
import com.vertex.domain.model.ARScanResult
import com.vertex.domain.model.DetectedPlane
import com.vertex.domain.model.LightingConditionData
import com.vertex.domain.model.Opening
import com.vertex.domain.model.PlaneOrientation
import com.vertex.domain.model.PlaneType
import com.vertex.domain.model.Position3D
import com.vertex.domain.model.Room
import com.vertex.domain.model.ScanWarning
import com.vertex.domain.model.ScanWarningType
import com.vertex.domain.model.Surface
import com.vertex.domain.model.SurfaceCondition
import com.vertex.domain.model.SurfaceDimensions
import com.vertex.domain.model.SurfaceMaterial
import com.vertex.domain.model.SurfaceTexture
import com.vertex.domain.model.SurfaceType
import com.vertex.domain.model.TrackingState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import platform.ARKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.datetime.Clock

/**
 * iOS implementation of ARManager using ARKit with full delegate support
 */
actual class ARManager {
    private var arSession: ARSession? = null
    private var sessionDelegate: ARSessionDelegateWrapper? = null
    private var isInitialized = false
    private val currentPlanes = mutableMapOf<String, ARPlaneAnchor>()

    actual suspend fun initialize(): Boolean {
        return try {
            when {
                !ARWorldTrackingConfiguration.isSupported -> {
                    println("ARKit: Device does not support AR")
                    false
                }
                else -> {
                    arSession = ARSession()
                    sessionDelegate = ARSessionDelegateWrapper()
                    arSession?.delegate = sessionDelegate
                    isInitialized = true
                    println("ARKit: Initialized successfully")
                    true
                }
            }
        } catch (e: Exception) {
            println("ARKit: Failed to initialize - ${e.message}")
            false
        }
    }

    actual suspend fun startSession(): com.vertex.ar.ARSession? {
        return if (isInitialized && arSession != null) {
            val configuration = createARConfiguration()
            arSession?.runWithConfiguration(configuration)
            println("ARKit: Session started")
            IOSARSessionImpl(arSession!!, sessionDelegate!!)
        } else {
            println("ARKit: Cannot start session - not initialized")
            null
        }
    }

    actual suspend fun scanRoom(): Flow<ARScanResult> = callbackFlow {
        val session = arSession ?: run {
            channel.close()
            return@callbackFlow
        }

        val delegate = sessionDelegate ?: run {
            channel.close()
            return@callbackFlow
        }

        // Setup delegate callbacks
        delegate.onPlaneAdded = { planeAnchor ->
            currentPlanes[planeAnchor.identifier.UUIDString] = planeAnchor
            emitScanResult()
        }

        delegate.onPlaneUpdated = { planeAnchor ->
            currentPlanes[planeAnchor.identifier.UUIDString] = planeAnchor
            emitScanResult()
        }

        delegate.onPlaneRemoved = { planeAnchor ->
            currentPlanes.remove(planeAnchor.identifier.UUIDString)
            emitScanResult()
        }

        delegate.onTrackingStateChanged = { camera ->
            emitScanResult()
        }

        // Helper function to emit scan results
        fun emitScanResult() {
            val frame = session.currentFrame
            val camera = frame?.camera
            val isTracking = camera?.trackingState == ARTrackingState.ARTrackingStateNormal

            val planes = currentPlanes.values.map { planeAnchor ->
                convertARPlaneAnchorToDetectedPlane(planeAnchor)
            }

            val progress = calculateScanProgress(planes)
            val roomArea = calculateRoomArea(planes)

            val lightEstimate = frame?.lightEstimate
            val lightingCondition = if (lightEstimate != null) {
                LightingConditionData(
                    ambientIntensity = lightEstimate.ambientIntensity.toFloat(),
                    colorTemperature = lightEstimate.ambientColorTemperature.toFloat(),
                    isAdequateForColorMatching = lightEstimate.ambientIntensity > 500
                )
            } else null

            val result = ARScanResult(
                scanProgress = progress,
                detectedPlanes = planes,
                roomArea = roomArea.toFloat(),
                lightingCondition = lightingCondition,
                trackingState = if (isTracking) TrackingState.NORMAL else TrackingState.LIMITED,
                scanQuality = if (isTracking) 0.8f else 0.3f,
                warnings = if (!isTracking) listOf(
                    ScanWarning(
                        type = ScanWarningType.TRACKING_LOST,
                        message = "Tracking lost. Move slowly."
                    )
                ) else emptyList()
            )

            trySend(result)
        }

        // Initial emit
        emitScanResult()

        awaitClose {
            // Cleanup callbacks
            delegate.onPlaneAdded = null
            delegate.onPlaneUpdated = null
            delegate.onPlaneRemoved = null
            delegate.onTrackingStateChanged = null
            currentPlanes.clear()
        }
    }

    actual suspend fun generateRoom(scanData: ARScanData): Room {
        val surfaces = mutableListOf<Surface>()
        val openings = mutableListOf<Opening>()

        // Group planes by type for better room reconstruction
        val wallPlanes = scanData.planes.filter { it.type == PlaneType.VERTICAL }
        val ceilingPlanes = scanData.planes.filter { it.type == PlaneType.HORIZONTAL_DOWN }

        // Process walls
        wallPlanes.forEach { plane ->
            surfaces.add(Surface(
                id = "surface_${plane.id}",
                type = SurfaceType.WALL,
                dimensions = SurfaceDimensions(
                    width = plane.width.toDouble(),
                    height = plane.height.toDouble()
                ),
                texture = SurfaceTexture.SMOOTH,
                currentColor = null,
                condition = SurfaceCondition.GOOD,
                material = SurfaceMaterial.DRYWALL
            ))
        }

        // Process ceilings
        ceilingPlanes.forEach { plane ->
            surfaces.add(Surface(
                id = "surface_${plane.id}",
                type = SurfaceType.CEILING,
                dimensions = SurfaceDimensions(
                    width = plane.width.toDouble(),
                    height = plane.height.toDouble()
                ),
                texture = SurfaceTexture.SMOOTH,
                currentColor = null,
                condition = SurfaceCondition.GOOD,
                material = SurfaceMaterial.DRYWALL
            ))
        }

        return Room(
            id = "ar_room_ios_${Clock.System.now().toEpochMilliseconds()}",
            name = "AR Scanned Room",
            dimensions = scanData.roomDimensions,
            surfaces = surfaces,
            openings = openings,
            createdAt = Clock.System.now()
        )
    }

    actual fun cleanup() {
        sessionDelegate?.cleanup()
        arSession?.pause()
        arSession?.delegate = null
        arSession = null
        sessionDelegate = null
        currentPlanes.clear()
        isInitialized = false
        println("ARKit: Cleaned up")
    }

    private fun createARConfiguration(): ARWorldTrackingConfiguration {
        return ARWorldTrackingConfiguration().apply {
            planeDetection = ARPlaneDetection.ARPlaneDetectionHorizontal or ARPlaneDetection.ARPlaneDetectionVertical
            isLightEstimationEnabled = true
            isAutoFocusEnabled = true

            if (ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstruction.ARSceneReconstructionMesh)) {
                sceneReconstruction = ARSceneReconstruction.ARSceneReconstructionMesh
                println("ARKit: LiDAR scene reconstruction enabled")
            }

            if (ARWorldTrackingConfiguration.supportsFrameSemantics(ARFrameSemantics.ARFrameSemanticSceneDepth)) {
                frameSemantics = ARFrameSemantics.ARFrameSemanticSceneDepth
                println("ARKit: Depth sensing enabled")
            }
        }
    }

    private fun convertARPlaneAnchorToDetectedPlane(anchor: ARPlaneAnchor): DetectedPlane {
        val planeType = when (anchor.alignment) {
            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> {
                val y = anchor.transform.columns.3.y
                if (y < 1.0) PlaneType.HORIZONTAL_UP else PlaneType.HORIZONTAL_DOWN
            }
            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> PlaneType.VERTICAL
            else -> PlaneType.UNKNOWN
        }

        val centerX = anchor.transform.columns.3.x
        val centerY = anchor.transform.columns.3.y
        val centerZ = anchor.transform.columns.3.z

        return DetectedPlane(
            id = anchor.identifier.UUIDString,
            type = planeType,
            area = anchor.extent.x * anchor.extent.z,
            center = Position3D(centerX, centerY, centerZ),
            orientation = when (anchor.alignment) {
                ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> PlaneOrientation.HORIZONTAL
                ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> PlaneOrientation.VERTICAL
                else -> PlaneOrientation.ANGLED
            }
        )
    }

    private fun calculateScanProgress(planes: List<DetectedPlane>): Float {
        val walls = planes.count { it.type == PlaneType.VERTICAL }
        val hasFloor = planes.any { it.type == PlaneType.HORIZONTAL_UP }
        val hasCeiling = planes.any { it.type == PlaneType.HORIZONTAL_DOWN }

        var progress = 0f
        progress += (walls.toFloat() / 4f).coerceAtMost(1f) * 0.6f
        if (hasFloor) progress += 0.2f
        if (hasCeiling) progress += 0.2f

        return progress.coerceAtMost(1f)
    }

    private fun calculateRoomArea(planes: List<DetectedPlane>): Double {
        return planes
            .filter { it.type == PlaneType.HORIZONTAL_UP }
            .maxByOrNull { it.area }
            ?.area?.toDouble() ?: 0.0
    }
}

/**
 * ARSession delegate wrapper for handling AR events
 */
class ARSessionDelegateWrapper : NSObject(), ARSessionDelegateProtocol {
    var onPlaneAdded: ((ARPlaneAnchor) -> Unit)? = null
    var onPlaneUpdated: ((ARPlaneAnchor) -> Unit)? = null
    var onPlaneRemoved: ((ARPlaneAnchor) -> Unit)? = null
    var onTrackingStateChanged: ((ARCamera) -> Unit)? = null
    var onSessionInterrupted: (() -> Unit)? = null
    var onSessionInterruptionEnded: (() -> Unit)? = null
    var onSessionFailed: ((NSError) -> Unit)? = null

    override fun session(session: ARSession, didAddAnchors: List<*>) {
        didAddAnchors.forEach { anchor ->
            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
                onPlaneAdded?.invoke(planeAnchor)
            }
        }
    }

    override fun session(session: ARSession, didUpdateAnchors: List<*>) {
        didUpdateAnchors.forEach { anchor ->
            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
                onPlaneUpdated?.invoke(planeAnchor)
            }
        }
    }

    override fun session(session: ARSession, didRemoveAnchors: List<*>) {
        didRemoveAnchors.forEach { anchor ->
            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
                onPlaneRemoved?.invoke(planeAnchor)
            }
        }
    }

    override fun session(session: ARSession, cameraDidChangeTrackingState: ARCamera) {
        onTrackingStateChanged?.invoke(cameraDidChangeTrackingState)

        when (cameraDidChangeTrackingState.trackingState) {
            ARTrackingState.ARTrackingStateNormal -> {
                println("ARKit: Tracking normal")
            }
            ARTrackingState.ARTrackingStateLimited -> {
                val reason = cameraDidChangeTrackingState.trackingStateReason
                println("ARKit: Tracking limited - reason: $reason")
            }
            ARTrackingState.ARTrackingStateNotAvailable -> {
                println("ARKit: Tracking not available")
            }
        }
    }

    override fun sessionWasInterrupted(session: ARSession) {
        println("ARKit: Session interrupted")
        onSessionInterrupted?.invoke()
    }

    override fun sessionInterruptionEnded(session: ARSession) {
        println("ARKit: Session interruption ended")
        onSessionInterruptionEnded?.invoke()
    }

    override fun session(session: ARSession, didFailWithError: NSError) {
        println("ARKit: Session failed - ${didFailWithError.localizedDescription}")
        onSessionFailed?.invoke(didFailWithError)
    }

    fun cleanup() {
        onPlaneAdded = null
        onPlaneUpdated = null
        onPlaneRemoved = null
        onTrackingStateChanged = null
        onSessionInterrupted = null
        onSessionInterruptionEnded = null
        onSessionFailed = null
    }
}

/**
 * iOS implementation of ARSession interface
 */
actual interface ARSession {
    actual fun isTracking(): Boolean
    actual fun getPlanes(): List<ARPlane>
    actual fun pause()
    actual fun resume()
    actual fun close()
}

/**
 * Complete iOS ARKit session implementation
 */
class IOSARSessionImpl(
    private val session: platform.ARKit.ARSession,
    private val delegate: ARSessionDelegateWrapper
) : com.vertex.ar.ARSession {

    override fun isTracking(): Boolean {
        val trackingState = session.currentFrame?.camera?.trackingState
        return trackingState == ARTrackingState.ARTrackingStateNormal
    }

    override fun getPlanes(): List<ARPlane> {
        val frame = session.currentFrame ?: return emptyList()

        return frame.anchors.mapNotNull { anchor ->
            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
                val planeType = when (planeAnchor.alignment) {
                    ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> {
                        val y = planeAnchor.transform.columns.3.y
                        if (y < 1.0) PlaneType.HORIZONTAL_UP else PlaneType.HORIZONTAL_DOWN
                    }
                    ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> PlaneType.VERTICAL
                    else -> PlaneType.UNKNOWN
                }

                ARPlane(
                    id = planeAnchor.identifier.UUIDString,
                    type = planeType,
                    width = planeAnchor.extent.x,
                    height = planeAnchor.extent.z,
                    area = planeAnchor.extent.x * planeAnchor.extent.z
                )
            }
        }
    }

    override fun pause() {
        session.pause()
        println("ARKit: Session paused")
    }

    override fun resume() {
        val configuration = ARWorldTrackingConfiguration().apply {
            planeDetection = ARPlaneDetection.ARPlaneDetectionHorizontal or ARPlaneDetection.ARPlaneDetectionVertical
            isLightEstimationEnabled = true

            if (ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstruction.ARSceneReconstructionMesh)) {
                sceneReconstruction = ARSceneReconstruction.ARSceneReconstructionMesh
            }
        }
        session.runWithConfiguration(configuration)
        println("ARKit: Session resumed")
    }

    override fun close() {
        delegate.cleanup()
        session.pause()
        println("ARKit: Session closed")
    }
}