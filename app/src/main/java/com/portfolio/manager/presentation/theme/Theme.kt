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
    onSurface = Color(0xFFFAFAFA),  // Zinc 50 - bright text
    surfaceVariant = Color(0xFF27272A),  // Zinc 800
    onSurfaceVariant = Color(0xFFA1A1AA),  // Zinc 400
    background = Color(0xFF09090B),  // Zinc 950 - deeper background
    onBackground = Color(0xFFFAFAFA),
    outline = Color(0xFF52525B),  // Zinc 600
    error = LossRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),  // Indigo 100
    onPrimaryContainer = Primary20,
    secondary = Secondary40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),  // Teal 100
    onSecondaryContainer = Secondary20,
    tertiary = Accent40,
    onTertiary = Color.Black,
    surface = Color.White,
    onSurface = Color(0xFF18181B),  // Zinc 900 - high contrast text
    surfaceVariant = Color(0xFFF4F4F5),  // Zinc 100
    onSurfaceVariant = Color(0xFF52525B),  // Zinc 600
    background = Color(0xFFFAFAFA),  // Zinc 50 - subtle off-white
    onBackground = Color(0xFF18181B),
    outline = Color(0xFFD4D4D8),  // Zinc 300
    error = LossRed,
    onError = Color.White
)

@Composable
fun FolioTheme(
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
