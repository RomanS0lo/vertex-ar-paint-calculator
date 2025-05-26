//// iosMain/kotlin/com/vertex/ar/ARManager.kt
//package com.vertex.ar
//
//import com.vertex.domain.model.*
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.flow.flow
//import platform.ARKit.*
//import platform.Foundation.*
//import platform.darwin.NSObject
//import kotlin.native.concurrent.AtomicReference
//import kotlin.native.concurrent.freeze
//
///**
// * iOS implementation of ARManager using ARKit with full delegate support
// */
//actual class ARManager {
//    private var arSession: platform.ARKit.ARSession? = null
//    private var sessionDelegate: ARSessionDelegateWrapper? = null
//    private var isInitialized = false
//    private val currentPlanes = mutableMapOf<String, ARPlaneAnchor>()
//
//    actual suspend fun initialize(): Boolean {
//        return try {
//            when {
//                !ARWorldTrackingConfiguration.isSupported -> {
//                    println("ARKit: Device does not support AR")
//                    false
//                }
//                else -> {
//                    arSession = platform.ARKit.ARSession()
//                    sessionDelegate = ARSessionDelegateWrapper()
//                    arSession?.delegate = sessionDelegate
//                    isInitialized = true
//                    println("ARKit: Initialized successfully")
//                    true
//                }
//            }
//        } catch (e: Exception) {
//            println("ARKit: Failed to initialize - ${e.message}")
//            false
//        }
//    }
//
//    actual suspend fun startSession(): ARSession? {
//        return if (isInitialized && arSession != null) {
//            val configuration = createARConfiguration()
//            arSession?.runWithConfiguration(configuration)
//            println("ARKit: Session started")
//            IOSARSessionImpl(arSession!!, sessionDelegate!!)
//        } else {
//            println("ARKit: Cannot start session - not initialized")
//            null
//        }
//    }
//
//    actual suspend fun scanRoom(): Flow<ARScanResult> = callbackFlow {
//        val session = arSession ?: run {
//            channel.close()
//            return@callbackFlow
//        }
//
//        val delegate = sessionDelegate ?: run {
//            channel.close()
//            return@callbackFlow
//        }
//
//        // Setup delegate callbacks
//        delegate.onPlaneAdded = { planeAnchor ->
//            currentPlanes[planeAnchor.identifier.UUIDString] = planeAnchor
//            emitScanResult()
//        }
//
//        delegate.onPlaneUpdated = { planeAnchor ->
//            currentPlanes[planeAnchor.identifier.UUIDString] = planeAnchor
//            emitScanResult()
//        }
//
//        delegate.onPlaneRemoved = { planeAnchor ->
//            currentPlanes.remove(planeAnchor.identifier.UUIDString)
//            emitScanResult()
//        }
//
//        delegate.onTrackingStateChanged = { camera ->
//            emitScanResult()
//        }
//
//        // Helper function to emit scan results
//        fun emitScanResult() {
//            val frame = session.currentFrame
//            val camera = frame?.camera
//            val isTracking = camera?.trackingState == ARTrackingState.ARTrackingStateNormal
//
//            val planes = currentPlanes.values.map { planeAnchor ->
//                convertARPlaneAnchorToARPlane(planeAnchor)
//            }
//
//            val progress = calculateScanProgress(planes)
//            val roomArea = calculateRoomArea(planes)
//
//            val result = ARScanResult(
//                isTracking = isTracking,
//                detectedPlanes = planes,
//                scanProgress = progress,
//                roomArea = roomArea,
//                lightingCondition = frame?.lightEstimate?.let {
//                    LightingCondition(
//                        ambientIntensity = it.ambientIntensity.toFloat(),
//                        ambientColorTemperature = it.ambientColorTemperature.toFloat()
//                    )
//                }
//            )
//
//            trySend(result)
//        }
//
//        // Initial emit
//        emitScanResult()
//
//        awaitClose {
//            // Cleanup callbacks
//            delegate.onPlaneAdded = null
//            delegate.onPlaneUpdated = null
//            delegate.onPlaneRemoved = null
//            delegate.onTrackingStateChanged = null
//            currentPlanes.clear()
//        }
//    }
//
//    actual suspend fun generateRoom(scanData: ARScanData): Room {
//        val surfaces = mutableListOf<Surface>()
//        val openings = mutableListOf<Opening>()
//
//        // Group planes by type for better room reconstruction
//        val wallPlanes = scanData.planes.filter { it.type == PlaneType.WALL }
//        val ceilingPlanes = scanData.planes.filter { it.type == PlaneType.CEILING }
//
//        // Process walls
//        wallPlanes.forEach { plane ->
//            surfaces.add(Surface(
//                id = "surface_${plane.id}",
//                type = SurfaceType.WALL,
//                dimensions = SurfaceDimensions(
//                    width = plane.width.toDouble(),
//                    height = plane.height.toDouble()
//                ),
//                texture = determineTextureFromConfidence(plane.confidence),
//                currentColor = null, // Would need color analysis
//                condition = SurfaceCondition.GOOD,
//                material = SurfaceMaterial.DRYWALL
//            ))
//        }
//
//        // Process ceilings
//        ceilingPlanes.forEach { plane ->
//            surfaces.add(Surface(
//                id = "surface_${plane.id}",
//                type = SurfaceType.CEILING,
//                dimensions = SurfaceDimensions(
//                    width = plane.width.toDouble(),
//                    height = plane.height.toDouble()
//                ),
//                texture = SurfaceTexture.SMOOTH,
//                currentColor = null,
//                condition = SurfaceCondition.GOOD,
//                material = SurfaceMaterial.DRYWALL
//            ))
//        }
//
//        // Detect openings (simplified - would need more sophisticated detection)
//        detectOpeningsInWalls(wallPlanes).forEach { opening ->
//            openings.add(opening)
//        }
//
//        return Room(
//            id = "ar_room_ios_${NSDate().timeIntervalSince1970.toLong()}",
//            name = "AR Scanned Room",
//            dimensions = scanData.roomDimensions,
//            surfaces = surfaces,
//            openings = openings,
//            createdAt = System.currentTimeMillis()
//        )
//    }
//
//    actual fun cleanup() {
//        sessionDelegate?.cleanup()
//        arSession?.pause()
//        arSession?.delegate = null
//        arSession = null
//        sessionDelegate = null
//        currentPlanes.clear()
//        isInitialized = false
//        println("ARKit: Cleaned up")
//    }
//
//    private fun createARConfiguration(): ARWorldTrackingConfiguration {
//        return ARWorldTrackingConfiguration().apply {
//            // Enable plane detection for all orientations
//            planeDetection = ARPlaneDetectionHorizontal or ARPlaneDetectionVertical
//
//            // Enable light estimation for better understanding of the environment
//            lightEstimationEnabled = true
//
//            // Enable auto focus for better tracking
//            isAutoFocusEnabled = true
//
//            // Check for LiDAR and enable scene reconstruction if available
//            if (ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstructionMesh)) {
//                sceneReconstruction = ARSceneReconstructionMesh
//                println("ARKit: LiDAR scene reconstruction enabled")
//            }
//
//            // Enable frame semantics if available (for better surface detection)
//            if (ARWorldTrackingConfiguration.supportsFrameSemantics(ARFrameSemanticSceneDepth)) {
//                frameSemantics = ARFrameSemanticSceneDepth
//                println("ARKit: Depth sensing enabled")
//            }
//        }
//    }
//
//    private fun convertARPlaneAnchorToARPlane(anchor: ARPlaneAnchor): ARPlane {
//        val planeType = when (anchor.alignment) {
//            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> {
//                // Determine if floor or ceiling based on height
//                val y = anchor.transform.columns.3.y
//                if (y < 1.0) PlaneType.FLOOR else PlaneType.CEILING
//            }
//            ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> PlaneType.WALL
//            else -> PlaneType.FLOOR
//        }
//
//        return ARPlane(
//            id = anchor.identifier.UUIDString,
//            type = planeType,
//            width = anchor.extent.x,
//            height = anchor.extent.z,
//            area = anchor.extent.x * anchor.extent.z,
//            confidence = when (anchor.classification) {
//                ARPlaneClassificationWall -> 0.9f
//                ARPlaneClassificationFloor -> 0.9f
//                ARPlaneClassificationCeiling -> 0.9f
//                ARPlaneClassificationTable -> 0.7f
//                ARPlaneClassificationSeat -> 0.7f
//                ARPlaneClassificationWindow -> 0.8f
//                ARPlaneClassificationDoor -> 0.8f
//                else -> 0.5f
//            }
//        )
//    }
//
//    private fun calculateScanProgress(planes: List<ARPlane>): Float {
//        val walls = planes.count { it.type == PlaneType.WALL }
//        val hasFloor = planes.any { it.type == PlaneType.FLOOR }
//        val hasCeiling = planes.any { it.type == PlaneType.CEILING }
//
//        var progress = 0f
//
//        // Need at least 3 walls for a room
//        progress += (walls.toFloat() / 4f).coerceAtMost(1f) * 0.6f
//
//        // Floor detection
//        if (hasFloor) progress += 0.2f
//
//        // Ceiling detection
//        if (hasCeiling) progress += 0.2f
//
//        return progress.coerceAtMost(1f)
//    }
//
//    private fun calculateRoomArea(planes: List<ARPlane>): Double {
//        return planes
//            .filter { it.type == PlaneType.FLOOR }
//            .maxByOrNull { it.area }
//            ?.area?.toDouble() ?: 0.0
//    }
//
//    private fun determineTextureFromConfidence(confidence: Float): SurfaceTexture {
//        return when {
//            confidence > 0.8f -> SurfaceTexture.SMOOTH
//            confidence > 0.6f -> SurfaceTexture.LIGHT_TEXTURE
//            confidence > 0.4f -> SurfaceTexture.MEDIUM_TEXTURE
//            else -> SurfaceTexture.HEAVY_TEXTURE
//        }
//    }
//
//    private fun detectOpeningsInWalls(wallPlanes: List<ARPlane>): List<Opening> {
//        // Simplified opening detection
//        // In a real implementation, this would analyze gaps in wall planes
//        return emptyList()
//    }
//}
//
///**
// * ARSession delegate wrapper for handling AR events
// */
//class ARSessionDelegateWrapper : NSObject(), ARSessionDelegateProtocol {
//    var onPlaneAdded: ((ARPlaneAnchor) -> Unit)? = null
//    var onPlaneUpdated: ((ARPlaneAnchor) -> Unit)? = null
//    var onPlaneRemoved: ((ARPlaneAnchor) -> Unit)? = null
//    var onTrackingStateChanged: ((ARCamera) -> Unit)? = null
//    var onSessionInterrupted: (() -> Unit)? = null
//    var onSessionInterruptionEnded: (() -> Unit)? = null
//    var onSessionFailed: ((NSError) -> Unit)? = null
//
//    override fun session(session: ARSession, didAddAnchors: List<*>) {
//        didAddAnchors.forEach { anchor ->
//            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
//                onPlaneAdded?.invoke(planeAnchor)
//            }
//        }
//    }
//
//    override fun session(session: ARSession, didUpdateAnchors: List<*>) {
//        didUpdateAnchors.forEach { anchor ->
//            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
//                onPlaneUpdated?.invoke(planeAnchor)
//            }
//        }
//    }
//
//    override fun session(session: ARSession, didRemoveAnchors: List<*>) {
//        didRemoveAnchors.forEach { anchor ->
//            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
//                onPlaneRemoved?.invoke(planeAnchor)
//            }
//        }
//    }
//
//    override fun session(session: ARSession, cameraDidChangeTrackingState: ARCamera) {
//        onTrackingStateChanged?.invoke(cameraDidChangeTrackingState)
//
//        // Log tracking state changes
//        when (cameraDidChangeTrackingState.trackingState) {
//            ARTrackingState.ARTrackingStateNormal -> {
//                println("ARKit: Tracking normal")
//            }
//            ARTrackingState.ARTrackingStateLimited -> {
//                val reason = cameraDidChangeTrackingState.trackingStateReason
//                println("ARKit: Tracking limited - reason: $reason")
//            }
//            ARTrackingState.ARTrackingStateNotAvailable -> {
//                println("ARKit: Tracking not available")
//            }
//        }
//    }
//
//    override fun sessionWasInterrupted(session: ARSession) {
//        println("ARKit: Session interrupted")
//        onSessionInterrupted?.invoke()
//    }
//
//    override fun sessionInterruptionEnded(session: ARSession) {
//        println("ARKit: Session interruption ended")
//        onSessionInterruptionEnded?.invoke()
//    }
//
//    override fun session(session: ARSession, didFailWithError: NSError) {
//        println("ARKit: Session failed - ${didFailWithError.localizedDescription}")
//        onSessionFailed?.invoke(didFailWithError)
//    }
//
//    fun cleanup() {
//        onPlaneAdded = null
//        onPlaneUpdated = null
//        onPlaneRemoved = null
//        onTrackingStateChanged = null
//        onSessionInterrupted = null
//        onSessionInterruptionEnded = null
//        onSessionFailed = null
//    }
//}
//
///**
// * iOS implementation of ARSession interface
// */
//actual interface ARSession {
//    actual fun isTracking(): Boolean
//    actual fun getPlanes(): List<ARPlane>
//    actual fun pause()
//    actual fun resume()
//    actual fun close()
//}
//
///**
// * Complete iOS ARKit session implementation
// */
//class IOSARSessionImpl(
//    private val session: platform.ARKit.ARSession,
//    private val delegate: ARSessionDelegateWrapper
//) : ARSession {
//
//    override fun isTracking(): Boolean {
//        val trackingState = session.currentFrame?.camera?.trackingState
//        return trackingState == ARTrackingState.ARTrackingStateNormal
//    }
//
//    override fun getPlanes(): List<ARPlane> {
//        val frame = session.currentFrame ?: return emptyList()
//
//        return frame.anchors.mapNotNull { anchor ->
//            (anchor as? ARPlaneAnchor)?.let { planeAnchor ->
//                val planeType = when (planeAnchor.alignment) {
//                    ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal -> {
//                        val y = planeAnchor.transform.columns.3.y
//                        if (y < 1.0) PlaneType.FLOOR else PlaneType.CEILING
//                    }
//                    ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentVertical -> PlaneType.WALL
//                    else -> PlaneType.FLOOR
//                }
//
//                ARPlane(
//                    id = planeAnchor.identifier.UUIDString,
//                    type = planeType,
//                    width = planeAnchor.extent.x,
//                    height = planeAnchor.extent.z,
//                    area = planeAnchor.extent.x * planeAnchor.extent.z,
//                    confidence = when (planeAnchor.classification) {
//                        ARPlaneClassificationWall -> 0.9f
//                        ARPlaneClassificationFloor -> 0.9f
//                        ARPlaneClassificationCeiling -> 0.9f
//                        else -> 0.7f
//                    }
//                )
//            }
//        }
//    }
//
//    override fun pause() {
//        session.pause()
//        println("ARKit: Session paused")
//    }
//
//    override fun resume() {
//        val configuration = ARWorldTrackingConfiguration().apply {
//            planeDetection = ARPlaneDetectionHorizontal or ARPlaneDetectionVertical
//            lightEstimationEnabled = true
//
//            if (ARWorldTrackingConfiguration.supportsSceneReconstruction(ARSceneReconstructionMesh)) {
//                sceneReconstruction = ARSceneReconstructionMesh
//            }
//        }
//        session.runWithConfiguration(configuration)
//        println("ARKit: Session resumed")
//    }
//
//    override fun close() {
//        delegate.cleanup()
//        session.pause()
//        println("ARKit: Session closed")
//    }
//}