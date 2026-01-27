package com.portfolio.manager.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A modern glassmorphism surface with frosted glass effect.
 * Provides a semi-transparent background with subtle gradient highlights
 * and a gradient border for depth.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Gradient for glass effect
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.9f),
            surfaceColor.copy(alpha = 0.75f)
        )
    )

    // Subtle highlight gradient for depth
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    // Gradient border for soft edges
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassGradient)
            .background(highlightGradient)
            .border(
                width = 1.dp,
                brush = borderGradient,
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
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
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
