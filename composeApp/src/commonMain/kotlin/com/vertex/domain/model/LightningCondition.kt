package com.vertex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LightingConditionData(
    val ambientIntensity: Float, // in lumens
    val colorTemperature: Float, // in Kelvin
    val isAdequateForColorMatching: Boolean
)
