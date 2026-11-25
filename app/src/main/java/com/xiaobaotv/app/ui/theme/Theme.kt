package com.xiaobaotv.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Dark scheme ──────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = Color(0xFF071227),
    primaryContainer = Color(0xFF1A2A4F),
    onPrimaryContainer = Color(0xFFDBE4FF),

    secondary = BrandAmber,
    onSecondary = Color(0xFF2A1600),
    secondaryContainer = Color(0xFF3A2A12),
    onSecondaryContainer = Color(0xFFFFDDB3),

    tertiary = BrandMint,
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF10332D),
    onTertiaryContainer = Color(0xFFBFF5E8),

    error = Destructive,
    onError = Color(0xFF2A0509),
    errorContainer = DestructiveContainerDark,
    onErrorContainer = Color(0xFFFFD9DC),

    background = Ink1,
    onBackground = TextHighDark,
    surface = Ink2,
    onSurface = TextHighDark,
    surfaceVariant = Ink4,
    onSurfaceVariant = TextMidDark,
    surfaceTint = BrandIndigo,

    surfaceBright = Ink5,
    surfaceDim = Ink0,
    surfaceContainerLowest = Color(0xFF080D17),
    surfaceContainerLow = Ink2,
    surfaceContainer = Ink3,
    surfaceContainerHigh = Ink4,
    surfaceContainerHighest = Ink5,

    outline = Color(0xFF3B4A63),
    outlineVariant = Color(0xFF23304A),
    inverseSurface = Color(0xFFEDF1FA),
    inverseOnSurface = Color(0xFF111826),
    inversePrimary = BrandIndigoDeep,
    scrim = Color(0xFF000000)
)

// ── Light scheme ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = BrandIndigoDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = Color(0xFF0A1633),

    secondary = BrandAmberDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBCF),
    onSecondaryContainer = Color(0xFF2A1600),

    tertiary = Color(0xFF0E8C8C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2FBF5),
    onTertiaryContainer = Color(0xFF00201D),

    error = Error,
    onError = Color.White,
    errorContainer = DestructiveContainerLight,
    onErrorContainer = Color(0xFF410002),

    background = Porcelain0,
    onBackground = TextHighLight,
    surface = Porcelain2,
    onSurface = TextHighLight,
    surfaceVariant = Porcelain4,
    onSurfaceVariant = TextMidLight,
    surfaceTint = BrandIndigoDeep,

    surfaceBright = Porcelain2,
    surfaceDim = Color(0xFFE4E9F3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Porcelain1,
    surfaceContainer = Porcelain3,
    surfaceContainerHigh = Porcelain4,
    surfaceContainerHighest = Porcelain5,

    outline = Color(0xFF9AA6BC),
    outlineVariant = Color(0xFFE2E7F2),
    inverseSurface = Color(0xFF111826),
    inverseOnSurface = Color(0xFFEDF1FA),
    inversePrimary = BrandIndigo,
    scrim = Color(0xFF000000)
)

@Composable
fun XiaobaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extras = if (darkTheme) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalXiaobaoColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XiaobaoTypography,
            shapes = XiaobaoShapes,
            content = content
        )
    }
}
