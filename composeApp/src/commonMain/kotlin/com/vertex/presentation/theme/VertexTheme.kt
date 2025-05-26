package com.vertex.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable


@Composable
fun VertexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        VertexDarkColors
    } else {
        VertexLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = VertexTypography,
        shapes = VertexShapes,
        content = content
    )
}