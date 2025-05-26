package com.vertex.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Tech-Forward Color Palette
object VertexColors {
    // Primary - Electric Cyan (Tech/Precision)
    val PrimaryCyan = Color(0xFF00D4FF)
    val PrimaryCyanDark = Color(0xFF006B7D)
    val PrimaryCyanContainer = Color(0xFF003544)
    val PrimaryCyanContainerLight = Color(0xFFB8F5FF)

    // Secondary - Electric Green (Accuracy/Success)
    val SecondaryGreen = Color(0xFF00FF88)
    val SecondaryGreenDark = Color(0xFF00695C)
    val SecondaryGreenContainer = Color(0xFF003322)
    val SecondaryGreenContainerLight = Color(0xFFB2F2E8)

    // Tertiary - Electric Orange (Warnings/Alerts)
    val TertiaryOrange = Color(0xFFFF6B00)
    val TertiaryOrangeDark = Color(0xFFE65100)
    val TertiaryOrangeContainer = Color(0xFF4D1F00)
    val TertiaryOrangeContainerLight = Color(0xFFFFE0CC)

    // Background - Deep Tech
    val BackgroundDark = Color(0xFF0A0E13)
    val BackgroundLight = Color(0xFFFAFBFC)
    val SurfaceDark = Color(0xFF111921)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantDark = Color(0xFF1A252E)
    val SurfaceVariantLight = Color(0xFFF1F5F9)

    // Text Colors
    val OnBackgroundDark = Color(0xFFE1E8ED)
    val OnBackgroundLight = Color(0xFF0F172A)
    val OnSurfaceVariantDark = Color(0xFF9BB5C4)
    val OnSurfaceVariantLight = Color(0xFF475569)

    // System Colors
    val OutlineDark = Color(0xFF334155)
    val OutlineLight = Color(0xFFCBD5E1)
    val ErrorDark = Color(0xFFFF453A)
    val ErrorLight = Color(0xFFDC2626)
}

val VertexDarkColors = darkColorScheme(
    primary = VertexColors.PrimaryCyan,
    onPrimary = Color.Black,
    primaryContainer = VertexColors.PrimaryCyanContainer,
    onPrimaryContainer = VertexColors.PrimaryCyan,

    secondary = VertexColors.SecondaryGreen,
    onSecondary = Color.Black,
    secondaryContainer = VertexColors.SecondaryGreenContainer,
    onSecondaryContainer = VertexColors.SecondaryGreen,

    tertiary = VertexColors.TertiaryOrange,
    onTertiary = Color.Black,
    tertiaryContainer = VertexColors.TertiaryOrangeContainer,
    onTertiaryContainer = VertexColors.TertiaryOrange,

    background = VertexColors.BackgroundDark,
    onBackground = VertexColors.OnBackgroundDark,
    surface = VertexColors.SurfaceDark,
    onSurface = VertexColors.OnBackgroundDark,
    surfaceVariant = VertexColors.SurfaceVariantDark,
    onSurfaceVariant = VertexColors.OnSurfaceVariantDark,

    outline = VertexColors.OutlineDark,
    error = VertexColors.ErrorDark,
    onError = Color.White
)

val VertexLightColors = lightColorScheme(
    primary = VertexColors.PrimaryCyanDark,
    onPrimary = Color.White,
    primaryContainer = VertexColors.PrimaryCyanContainerLight,
    onPrimaryContainer = VertexColors.PrimaryCyanDark,

    secondary = VertexColors.SecondaryGreenDark,
    onSecondary = Color.White,
    secondaryContainer = VertexColors.SecondaryGreenContainerLight,
    onSecondaryContainer = VertexColors.SecondaryGreenDark,

    tertiary = VertexColors.TertiaryOrangeDark,
    onTertiary = Color.White,
    tertiaryContainer = VertexColors.TertiaryOrangeContainerLight,
    onTertiaryContainer = VertexColors.TertiaryOrangeDark,

    background = VertexColors.BackgroundLight,
    onBackground = VertexColors.OnBackgroundLight,
    surface = VertexColors.SurfaceLight,
    onSurface = VertexColors.OnBackgroundLight,
    surfaceVariant = VertexColors.SurfaceVariantLight,
    onSurfaceVariant = VertexColors.OnSurfaceVariantLight,

    outline = VertexColors.OutlineLight,
    error = VertexColors.ErrorLight,
    onError = Color.White
)