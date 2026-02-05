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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation levels for glass surfaces.
 */
object GlassElevation {
    val None = 0.dp
    val Low = 2.dp
    val Medium = 4.dp
    val High = 8.dp
}

/**
 * A modern glassmorphism surface with frosted glass effect.
 * Provides a semi-transparent background with subtle gradient highlights,
 * a gradient border for depth, and optional elevation shadow.
 *
 * @param modifier Modifier to be applied to the surface
 * @param shape Shape of the surface (default: RoundedCornerShape(20.dp))
 * @param elevation Shadow elevation for depth (default: Low)
 * @param shadowColor Optional color tint for the shadow (for gain/loss effects)
 * @param content Content to be displayed inside the surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = GlassElevation.Low,
    shadowColor: Color = Color.Black,
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

    // Subtle highlight gradient for depth (inner glow at top)
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
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
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor.copy(alpha = 0.15f),
                spotColor = shadowColor.copy(alpha = 0.25f)
            )
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
 *
 * @param modifier Modifier to be applied to the card
 * @param shape Shape of the card (default: RoundedCornerShape(16.dp))
 * @param elevation Shadow elevation for depth (default: None)
 * @param content Content to be displayed inside the card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = GlassElevation.None,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                } else {
                    Modifier
                }
            )
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

/**
 * An elevated glass surface variant with more prominent shadow.
 * Use for cards that need to stand out more.
 */
@Composable
fun ElevatedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    shadowColor: Color = Color.Black,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        elevation = GlassElevation.High,
        shadowColor = shadowColor,
        content = content
    )
}
