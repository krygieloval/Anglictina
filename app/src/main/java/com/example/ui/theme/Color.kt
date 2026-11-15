package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors (Playful Storybook Indigo & Sky)
val IndigoPrimary = Color(0xFF3F45D7)
val IndigoLightContainer = Color(0xFFE6E8FF)
val IndigoDarkText = Color(0xFF1A1E7A)

val CoralSecondary = Color(0xFFFF6B4A)
val CoralContainer = Color(0xFFFFE5DF)
val CoralDarkText = Color(0xFF7D2210)

val MintTertiary = Color(0xFF0EA572)
val MintContainer = Color(0xFFD5F9EC)
val MintDarkText = Color(0xFF065037)

val SunnyAmber = Color(0xFFF59E0B)
val SunnyAmberContainer = Color(0xFFFEF3C7)
val SunnyAmberText = Color(0xFF78350F)

val LavenderAccent = Color(0xFF7C3AED)
val LavenderContainer = Color(0xFFEDE9FE)

val SkyBackground = Color(0xFFF8FAFF)
val CreamSurface = Color(0xFFFFFFFF)
val SoftSurfaceVariant = Color(0xFFEEF2FF)
val OutlineSoft = Color(0xFFD0D7F0)
val InkDark = Color(0xFF1E293B)
val InkMuted = Color(0xFF526079)

val ErrorRed = Color(0xFFDC2626)
val ErrorContainerLight = Color(0xFFFEE2E2)

// Dark theme tokens
val DarkBackground = Color(0xFF101325)
val DarkSurface = Color(0xFF191D36)
val DarkSurfaceVariant = Color(0xFF252A4A)
val IndigoLightForDark = Color(0xFFB4B9FF)
val CoralLightForDark = Color(0xFFFF9B84)
val MintLightForDark = Color(0xFF6EE7B7)

// Pack theme accents
data class PackPalette(
    val container: Color,
    val accent: Color,
    val onContainer: Color
)

val PackPalettes = listOf(
    PackPalette(Color(0xFFE0F2FE), Color(0xFF0284C7), Color(0xFF0C4A6E)), // Sky
    PackPalette(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFF78350F)), // Amber
    PackPalette(Color(0xFFD1FAE5), Color(0xFF059669), Color(0xFF064E3B)), // Mint
    PackPalette(Color(0xFFFCE7F3), Color(0xFFDB2777), Color(0xFF831843)), // Berry
    PackPalette(Color(0xFFEDE9FE), Color(0xFF7C3AED), Color(0xFF4C1D95)), // Violet
    PackPalette(Color(0xFFFFEDD5), Color(0xFFEA580C), Color(0xFF7C2D12)), // Tangerine
)
