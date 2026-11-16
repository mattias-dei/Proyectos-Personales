package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WinterDarkColorScheme = darkColorScheme(
    primary = HollyCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = HollyCrimsonContainerDark,
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = FairyGoldDark,
    onSecondary = Color(0xFF231900),
    secondaryContainer = FairyGoldContainerDark,
    onSecondaryContainer = Color(0xFFFFE59E),
    tertiary = PineEmeraldDark,
    onTertiary = Color(0xFF002415),
    tertiaryContainer = PineEmeraldContainerDark,
    onTertiaryContainer = Color(0xFFB7F3D1),
    background = WinterBg,
    onBackground = WinterTextPrimary,
    surface = WinterSurface,
    onSurface = WinterTextPrimary,
    surfaceVariant = WinterSurfaceVariant,
    onSurfaceVariant = WinterTextSecondary,
    outline = WinterOutline
)

private val SnowLightColorScheme = lightColorScheme(
    primary = HollyCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = HollyCrimsonContainerLight,
    onPrimaryContainer = Color(0xFF410008),
    secondary = FairyGoldLight,
    onSecondary = Color.White,
    secondaryContainer = FairyGoldContainerLight,
    onSecondaryContainer = Color(0xFF2C1B00),
    tertiary = PineEmeraldLight,
    onTertiary = Color.White,
    tertiaryContainer = PineEmeraldContainerLight,
    onTertiaryContainer = Color(0xFF002111),
    background = SnowBg,
    onBackground = SnowTextPrimary,
    surface = SnowSurface,
    onSurface = SnowTextPrimary,
    surfaceVariant = SnowSurfaceVariant,
    onSurfaceVariant = SnowTextSecondary,
    outline = SnowOutline
)

@Composable
fun NavidadBuzzerTheme(
    darkWinterMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkWinterMode) WinterDarkColorScheme else SnowLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
