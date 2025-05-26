// commonMain/kotlin/com/vertex/presentation/arscan/ARScanViewModel.kt
package com.vertex.presentation.arscan

import com.vertex.ar.ARManager
import com.vertex.ar.ARSession
import com.vertex.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for AR room scanning functionality
 * This can be used with Compose Multiplatform or platform-specific UI
 */
class ARScanViewModel(
    private val arManager: ARManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    // UI State
    private val _uiState = MutableStateFlow(ARScanUiState())
    val uiState: StateFlow<ARScanUiState> = _uiState.asStateFlow()

    // Current AR session
    private var currentSession: ARSession? = null
    private var scanJob: Job? = null

    // Scanned data accumulator
    private val detectedPlanes = mutableListOf<ARPlane>()
    private var roomDimensions: RoomDimensions? = null

    init {
        initializeAR()
    }

    private fun initializeAR() {
        scope.launch {
            _uiState.update { it.copy(isInitializing = true) }

            val initialized = arManager.initialize()

            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isARAvailable = initialized,
                    errorMessage = if (!initialized) "AR is not available on this device" else null
                )
            }
        }
    }

    fun startScanning() {
        scope.launch {
            try {
                _uiState.update { it.copy(isStartingSession = true, errorMessage = null) }

                currentSession = arManager.startSession()

                if (currentSession != null) {
                    _uiState.update {
                        it.copy(
                            isStartingSession = false,
                            isScanning = true,
                            scanningState = ScanningState.SCANNING
                        )
                    }

                    // Start collecting scan data
                    scanJob = scope.launch {
                        arManager.scanRoom()
                            .catch { error ->
                                _uiState.update {
                                    it.copy(
                                        errorMessage = "Scanning error: ${error.message}",
                                        scanningState = ScanningState.ERROR
                                    )
                                }
                            }
                            .collect { scanResult ->
                                handleScanResult(scanResult)
                            }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isStartingSession = false,
                            errorMessage = "Failed to start AR session"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStartingSession = false,
                        errorMessage = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleScanResult(scanResult: ARScanResult) {
        // Update detected planes
        detectedPlanes.clear()
        detectedPlanes.addAll(scanResult.detectedPlanes)

        // Estimate room dimensions from planes
        roomDimensions = estimateRoomDimensionsFromPlanes(scanResult.detectedPlanes)

        // Update UI state
        _uiState.update { currentState ->
            currentState.copy(
                currentScanResult = scanResult,
                scanProgress = scanResult.scanProgress,
                detectedSurfaces = scanResult.detectedPlanes.size,
                roomArea = scanResult.roomArea,
                lightingQuality = when (scanResult.lightingCondition?.ambientIntensity) {
                    null -> LightingQuality.UNKNOWN
                    in 0f..500f -> LightingQuality.POOR
                    in 500f..1000f -> LightingQuality.FAIR
                    else -> LightingQuality.GOOD
                },
                canFinishScanning = scanResult.scanProgress > 0.7f
            )
        }

        // Auto-complete scanning if we have enough data
        if (scanResult.scanProgress >= 0.95f && uiState.value.scanningState == ScanningState.SCANNING) {
            finishScanning()
        }
    }

    fun pauseScanning() {
        currentSession?.pause()
        _uiState.update {
            it.copy(scanningState = ScanningState.PAUSED)
        }
    }

    fun resumeScanning() {
        currentSession?.resume()
        _uiState.update {
            it.copy(scanningState = ScanningState.SCANNING)
        }
    }

    fun finishScanning() {
        scope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    scanningState = ScanningState.PROCESSING
                )
            }

            try {
                // Create scan data
                val scanData = ARScanData(
                    planes = detectedPlanes.toList(),
                    roomDimensions = roomDimensions ?: RoomDimensions(3.0, 4.0, 2.5),
                    confidence = _uiState.value.scanProgress
                )

                // Generate room from scan data
                val room = arManager.generateRoom(scanData)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        scanningState = ScanningState.COMPLETED,
                        scannedRoom = room,
                        isScanning = false
                    )
                }

                // Stop scanning
                stopScanning()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to process scan: ${e.message}",
                        scanningState = ScanningState.ERROR
                    )
                }
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        currentSession?.close()
        currentSession = null

        _uiState.update {
            it.copy(
                isScanning = false,
                scanningState = ScanningState.IDLE
            )
        }
    }

    fun resetScan() {
        stopScanning()
        detectedPlanes.clear()
        roomDimensions = null

        _uiState.update { ARScanUiState(isARAvailable = it.isARAvailable) }
    }

    fun cleanup() {
        stopScanning()
        arManager.cleanup()
    }

    private fun estimateRoomDimensionsFromPlanes(planes: List<ARPlane>): RoomDimensions {
        val walls = planes.filter { it.type == PlaneType.WALL }
        val floors = planes.filter { it.type == PlaneType.FLOOR }

        // Simple estimation - in production, use more sophisticated algorithms
        val width = walls.maxOfOrNull { it.width } ?: 3.0
        val length = floors.maxOfOrNull { it.width } ?: 4.0
        val height = walls.maxOfOrNull { it.height } ?: 2.5

        return RoomDimensions(
            width = width.toDouble(),
            length = length.toDouble(),
            height = height.toDouble()
        )
    }
}

/**
 * UI State for AR scanning
 */
data class ARScanUiState(
    // Initialization
    val isInitializing: Boolean = false,
    val isARAvailable: Boolean = false,

    // Session state
    val isStartingSession: Boolean = false,
    val isScanning: Boolean = false,
    val scanningState: ScanningState = ScanningState.IDLE,

    // Scan progress
    val scanProgress: Float = 0f,
    val detectedSurfaces: Int = 0,
    val roomArea: Double = 0.0,
    val lightingQuality: LightingQuality = LightingQuality.UNKNOWN,

    // Current scan data
    val currentScanResult: ARScanResult? = null,

    // Processing
    val isProcessing: Boolean = false,
    val canFinishScanning: Boolean = false,

    // Results
    val scannedRoom: Room? = null,

    // Error handling
    val errorMessage: String? = null
)

enum class ScanningState {
    IDLE,
    SCANNING,
    PAUSED,
    PROCESSING,
    COMPLETED,
    ERROR
}

enum class LightingQuality {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD
}

/**
 * Instructions for different scanning states
 * @Suppress("EXTENSION_SHADOWED_BY_MEMBER") - For iOS interop
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun ScanningState.getInstructions(): String {
    return when (this) {
        ScanningState.IDLE -> "Position your device to start scanning"
        ScanningState.SCANNING -> "Move slowly around the room to scan all walls"
        ScanningState.PAUSED -> "Scanning paused - tap to resume"
        ScanningState.PROCESSING -> "Processing scan data..."
        ScanningState.COMPLETED -> "Scan complete!"
        ScanningState.ERROR -> "Scan failed - please try again"
    }
}

/**
 * Scanning tips based on progress
 */
fun getScanningTip(progress: Float, detectedSurfaces: Int): String {
    return when {
        progress < 0.2f -> "Point your device at the walls to start detecting surfaces"
        progress < 0.4f && detectedSurfaces < 2 -> "Try to scan more walls - rotate slowly"
        progress < 0.6f -> "Good! Keep scanning to detect all surfaces"
        progress < 0.8f -> "Almost there! Make sure to scan the ceiling if needed"
        else -> "Excellent! You can finish scanning now"
    }
}