package com.portfolio.manager.presentation.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A modern glassmorphism surface with frosted glass effect.
 * Uses blur on API 31+ and falls back to gradient overlay on older versions.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blur: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    // Gradient for glass effect
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.85f),
            surfaceColor.copy(alpha = 0.75f)
        )
    )

    // Subtle highlight gradient for depth
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blur)
                } else {
                    Modifier
                }
            )
            .background(glassGradient)
            .background(highlightGradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.3f),
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * A lighter glass card variant for nested content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.5f),
                        surfaceColor.copy(alpha = 0.3f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = shape
            )
    ) {
        content()
    }
}
