@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.vertex.ar

import com.vertex.domain.model.ARPlane
import com.vertex.domain.model.ARScanData
import com.vertex.domain.model.ARScanResult
import com.vertex.domain.model.Room
import kotlinx.coroutines.flow.Flow

expect class ARManager {
    suspend fun initialize(): Boolean
    suspend fun startSession(): ARSession?
    suspend fun scanRoom(): Flow<ARScanResult>
    suspend fun generateRoom(scanData: ARScanData): Room
    fun cleanup()
}

