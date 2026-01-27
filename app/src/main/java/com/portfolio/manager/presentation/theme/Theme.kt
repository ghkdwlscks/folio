package com.portfolio.manager.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary60,
    onPrimary = Color.White,
    primaryContainer = Primary20,
    onPrimaryContainer = Primary80,
    secondary = Secondary60,
    onSecondary = Color.White,
    secondaryContainer = Secondary20,
    onSecondaryContainer = Secondary80,
    tertiary = Accent80,
    onTertiary = Color.Black,
    surface = SurfaceDark,
    onSurface = Color(0xFFE2E8F0),  // Slate 200
    surfaceVariant = Color(0xFF1E293B),  // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8),  // Slate 400
    background = SurfaceDark,
    onBackground = Color(0xFFE2E8F0),
    outline = Color(0xFF475569)  // Slate 600
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Color.White,
    primaryContainer = Primary80,
    onPrimaryContainer = Primary20,
    secondary = Secondary40,
    onSecondary = Color.White,
    secondaryContainer = Secondary80,
    onSecondaryContainer = Secondary20,
    tertiary = Accent40,
    onTertiary = Color.Black,
    surface = SurfaceLight,
    onSurface = Color(0xFF1E293B),  // Slate 800
    surfaceVariant = Color(0xFFE2E8F0),  // Slate 200
    onSurfaceVariant = Color(0xFF64748B),  // Slate 500
    background = SurfaceLight,
    onBackground = Color(0xFF1E293B),
    outline = Color(0xFFCBD5E1)  // Slate 300
)

@Composable
fun PortfolioManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
