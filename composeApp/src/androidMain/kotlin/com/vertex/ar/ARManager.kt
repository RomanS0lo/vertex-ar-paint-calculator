// androidMain/kotlin/com/vertex/ar/ARManager.kt
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.vertex.ar

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*

import com.vertex.domain.model.ARPlane
import com.vertex.domain.model.ARScanData
import com.vertex.domain.model.ARScanResult
import com.vertex.domain.model.DetectedPlane
import com.vertex.domain.model.LightingConditionData
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlin.coroutines.coroutineContext

/**
 * Android implementation of ARManager using ARCore
 */
actual class ARManager(private val context: Context) {
    private var arSession: Session? = null
    private var isInitialized = false

    actual suspend fun initialize(): Boolean {
        return try {
            when (ArCoreApk.getInstance().checkAvailability(context)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    initializeARCore()
                    true
                }
                else -> {
                    Log.w("ARManager", "ARCore not supported or needs update")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ARManager", "Failed to initialize ARCore", e)
            false
        }
    }

    private fun initializeARCore() {
        arSession = Session(context).apply {
            val config = Config(this).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                depthMode = Config.DepthMode.AUTOMATIC
                instantPlacementMode = Config.InstantPlacementMode.DISABLED
            }
            configure(config)
        }
        isInitialized = true
        Log.i("ARManager", "ARCore initialized successfully")
    }

    actual suspend fun startSession(): ARSession? {
        return if (isInitialized && arSession != null) {
            try {
                arSession?.resume()
                AndroidARSession(arSession!!)
            } catch (e: CameraNotAvailableException) {
                Log.e("ARManager", "Camera not available", e)
                null
            }
        } else {
            null
        }
    }

    actual suspend fun scanRoom(): Flow<ARScanResult> = flow {
        val session = arSession ?: return@flow

        while (coroutineContext.isActive) {
            try {
                val frame = session.update()
                val camera = frame.camera

                if (camera.trackingState == TrackingState.TRACKING) {
                    val planes = session.getAllTrackables(Plane::class.java)
                        .filter { it.trackingState == TrackingState.TRACKING }
                        .map { convertPlaneToDetectedPlane(it) }

                    val progress = calculateScanProgress(planes)
                    val roomArea = calculateRoomArea(planes)

                    // Get light estimate if available
                    val lightEstimate = frame.lightEstimate
                    val lightingCondition = if (true) {
                        LightingConditionData(
                            ambientIntensity = lightEstimate.pixelIntensity,
                            colorTemperature = lightEstimate.colorCorrection[0] * 6500f, // Approximate
                            isAdequateForColorMatching = lightEstimate.pixelIntensity > 0.3f
                        )
                    } else null

                    emit(ARScanResult(
                        scanProgress = progress,
                        detectedPlanes = planes,
                        roomArea = roomArea.toFloat(),
                        lightingCondition = lightingCondition,
                        trackingState = TrackingState.NORMAL,
                        scanQuality = if (progress > 0.5f) 0.8f else 0.5f,
                        warnings = emptyList()
                    ))
                } else {
                    emit(ARScanResult(
                        scanProgress = 0f,
                        detectedPlanes = emptyList(),
                        roomArea = 0f,
                        lightingCondition = null,
                        trackingState = TrackingState.NOT_AVAILABLE,
                        scanQuality = 0f,
                        warnings = listOf(ScanWarning(
                            type = ScanWarningType.TRACKING_LOST,
                            message = "Tracking lost. Move slowly and ensure good lighting."
                        ))
                    ))
                }
            } catch (e: Exception) {
                Log.e("ARManager", "Error during scanning", e)
            }

            delay(100) // Scan update rate
        }
    }

    actual suspend fun generateRoom(scanData: ARScanData): Room {
        val walls = scanData.planes.filter { it.type == PlaneType.VERTICAL }
        val ceilings = scanData.planes.filter { it.type == PlaneType.HORIZONTAL_DOWN }
        // Note: We typically don't paint floors, so filtering them out

        val surfaces = mutableListOf<Surface>()

        // Add wall surfaces
        walls.forEach { plane ->
            surfaces.add(Surface(
                id = "surface_${plane.id}",
                type = SurfaceType.WALL,
                dimensions = SurfaceDimensions(
                    width = plane.width.toDouble(),
                    height = plane.height.toDouble()
                ),
                texture = SurfaceTexture.SMOOTH, // Default, can be enhanced with texture analysis
                currentColor = null, // To be determined by color analysis
                condition = SurfaceCondition.GOOD, // Default, can be enhanced with image analysis
                material = SurfaceMaterial.DRYWALL // Default, most common
            ))
        }

        // Add ceiling surfaces
        ceilings.forEach { plane ->
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
            id = "ar_room_${Clock.System.now().toEpochMilliseconds()}",
            name = "AR Scanned Room",
            dimensions = scanData.roomDimensions,
            surfaces = surfaces,
            openings = emptyList(), // To be detected in future enhancement
            createdAt = Clock.System.now()
        )
    }

    actual fun cleanup() {
        arSession?.pause()
        arSession?.close()
        arSession = null
        isInitialized = false
    }

    private fun convertPlaneToDetectedPlane(plane: Plane): DetectedPlane {
        val planeType = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneType.HORIZONTAL_UP
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneType.HORIZONTAL_DOWN
            Plane.Type.VERTICAL -> PlaneType.VERTICAL
        }

        val centerPose = plane.centerPose
        val translation = centerPose.translation

        return DetectedPlane(
            id = plane.hashCode().toString(),
            type = planeType,
            area = plane.extentX * plane.extentZ,
            center = Position3D(
                x = translation[0],
                y = translation[1],
                z = translation[2]
            ),
            orientation = when (plane.type) {
                Plane.Type.HORIZONTAL_UPWARD_FACING,
                Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneOrientation.HORIZONTAL
                Plane.Type.VERTICAL -> PlaneOrientation.VERTICAL
                else -> PlaneOrientation.ANGLED
            }
        )
    }

    private fun calculateScanProgress(planes: List<DetectedPlane>): Float {
        val walls = planes.count { it.type == PlaneType.VERTICAL }
        val floors = planes.count { it.type == PlaneType.HORIZONTAL_UP }

        // Progress calculation: need at least 4 walls and 1 floor for a complete room
        val wallProgress = (walls.toFloat() / 4f).coerceAtMost(1f) * 0.7f
        val floorProgress = if (floors > 0) 0.3f else 0f

        return (wallProgress + floorProgress).coerceAtMost(1f)
    }

    private fun calculateRoomArea(planes: List<DetectedPlane>): Double {
        return planes
            .filter { it.type == PlaneType.HORIZONTAL_UP }
            .sumOf { it.area.toDouble() }
    }
}

/**
 * Android implementation of ARSession interface
 */
actual interface ARSession {
    actual fun isTracking(): Boolean
    actual fun getPlanes(): List<ARPlane>
    actual fun pause()
    actual fun resume()
    actual fun close()
}

/**
 * Android ARCore session implementation
 */
class AndroidARSession(private val session: Session) : ARSession {

    override fun isTracking(): Boolean {
        return try {
            val frame = session.update()
            frame.camera.trackingState == TrackingState.TRACKING
        } catch (e: Exception) {
            false
        }
    }

    override fun getPlanes(): List<ARPlane> {
        return try {
            session.getAllTrackables(Plane::class.java)
                .filter { it.trackingState == TrackingState.TRACKING }
                .map { plane ->
                    val planeType = when (plane.type) {
                        Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneType.HORIZONTAL_UP
                        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneType.HORIZONTAL_DOWN
                        Plane.Type.VERTICAL -> PlaneType.VERTICAL
                    }

                    ARPlane(
                        id = plane.hashCode().toString(),
                        type = planeType,
                        width = plane.extentX,
                        height = plane.extentZ,
                        area = plane.extentX * plane.extentZ
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun pause() {
        session.pause()
    }

    override fun resume() {
        try {
            session.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e("AndroidARSession", "Camera not available", e)
        }
    }

    override fun close() {
        session.close()
    }
}