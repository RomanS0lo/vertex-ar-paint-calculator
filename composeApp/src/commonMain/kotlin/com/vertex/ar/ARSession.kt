package com.vertex.ar

import com.vertex.domain.model.ARPlane

expect interface ARSession {
    fun isTracking(): Boolean
    fun getPlanes(): List<ARPlane>
    fun pause()
    fun resume()
    fun close()
}