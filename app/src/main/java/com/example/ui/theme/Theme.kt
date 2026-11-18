package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AccentPalette
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ThemeMode

@Immutable
data class HardikLuxuryTokens(
    val themeStyle: AppThemeStyle,
    val isDark: Boolean,
    val accent: Color,
    val glassSurface: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val shimmerHighlight: Color
)

val LocalLuxuryTokens = staticCompositionLocalOf {
    HardikLuxuryTokens(
        themeStyle = AppThemeStyle.LIQUID_GLASS,
        isDark = true,
        accent = Color(0xFF4BE2FF),
        glassSurface = GlassCardDark,
        glassBorderTop = Color(0x55FFFFFF),
        glassBorderBottom = Color(0x18FFFFFF),
        backgroundTop = GlassNightTop,
        backgroundBottom = GlassNightBottom,
        shimmerHighlight = Color(0x284BE2FF)
    )
}

@Composable
fun HardikBudsProTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.LIQUID_GLASS,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.LIQUID_CYAN,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val rawAccent = Color(accentPalette.colorHex)
    val effectiveAccent = if (!isDark && accentPalette == AccentPalette.PLATINUM_SILVER) {
        Color(0xFF2B3548)
    } else {
        rawAccent
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = effectiveAccent,
            onPrimary = if (accentPalette == AccentPalette.PLATINUM_SILVER || accentPalette == AccentPalette.LIQUID_CYAN || accentPalette == AccentPalette.TITANIUM_GOLD) Color(0xFF080A0F) else Color.White,
            secondary = SilverBrushed,
            onSecondary = ObsidianDeep,
            tertiary = StatusEmerald,
            background = if (themeStyle == AppThemeStyle.LIQUID_GLASS) GlassNightTop else ObsidianDeep,
            onBackground = SilverPlatinum,
            surface = if (themeStyle == AppThemeStyle.LIQUID_GLASS) GlassCardDark else ObsidianSurface,
            onSurface = SilverPlatinum,
            surfaceVariant = ObsidianElevated,
            onSurfaceVariant = SilverBrushed,
            outline = SilverHairlineDark,
            error = StatusRose
        )
    } else {
        lightColorScheme(
            primary = effectiveAccent,
            onPrimary = Color.White,
            secondary = SlateMuted,
            onSecondary = Color.White,
            tertiary = StatusEmerald,
            background = PearlBackground,
            onBackground = InkCharcoal,
            surface = PearlSurface,
            onSurface = InkCharcoal,
            surfaceVariant = PearlElevated,
            onSurfaceVariant = SlateMuted,
            outline = SilverHairlineLight,
            error = StatusRose
        )
    }

    val luxuryTokens = when {
        themeStyle == AppThemeStyle.LIQUID_GLASS && isDark -> HardikLuxuryTokens(
            themeStyle = themeStyle,
            isDark = true,
            accent = effectiveAccent,
            glassSurface = Color(0xAA131C2E),
            glassBorderTop = Color(0x52FFFFFF),
            glassBorderBottom = Color(0x1694B8FF),
            backgroundTop = Color(0xFF070C17),
            backgroundBottom = Color(0xFF0D1526),
            shimmerHighlight = effectiveAccent.copy(alpha = 0.20f)
        )
        themeStyle == AppThemeStyle.LIQUID_GLASS && !isDark -> HardikLuxuryTokens(
            themeStyle = themeStyle,
            isDark = false,
            accent = effectiveAccent,
            glassSurface = Color(0xD8FFFFFF),
            glassBorderTop = Color(0xFFFFFFFF),
            glassBorderBottom = Color(0x336C82A6),
            backgroundTop = Color(0xFFEAF0FA),
            backgroundBottom = Color(0xFFF6F9FF),
            shimmerHighlight = effectiveAccent.copy(alpha = 0.16f)
        )
        themeStyle == AppThemeStyle.BLACK_SILVER_LUXURY && isDark -> HardikLuxuryTokens(
            themeStyle = themeStyle,
            isDark = true,
            accent = effectiveAccent,
            glassSurface = Color(0xEB101218),
            glassBorderTop = Color(0x5CE2E8F1),
            glassBorderBottom = Color(0x1E697386),
            backgroundTop = Color(0xFF050608),
            backgroundBottom = Color(0xFF0F1117),
            shimmerHighlight = SilverPlatinum.copy(alpha = 0.14f)
        )
        else -> HardikLuxuryTokens(
            themeStyle = themeStyle,
            isDark = false,
            accent = effectiveAccent,
            glassSurface = Color(0xF5FFFFFF),
            glassBorderTop = Color(0x66404A5E),
            glassBorderBottom = Color(0x28404A5E),
            backgroundTop = Color(0xFFEDF0F5),
            backgroundBottom = Color(0xFFF8F9FC),
            shimmerHighlight = Color(0x182B3548)
        )
    }

    CompositionLocalProvider(LocalLuxuryTokens provides luxuryTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HardikTypography,
            content = content
        )
    }
}
