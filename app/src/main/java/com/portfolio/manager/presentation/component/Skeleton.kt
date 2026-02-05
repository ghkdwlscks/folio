package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonDashboard(
    modifier: Modifier = Modifier
) {
    // Smoother shimmer with wider gradient and softer easing
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Wider gradient spread for smoother visual
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 600f, translateAnim - 600f),
        end = Offset(translateAnim, translateAnim)
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SkeletonPortfolioSummary(brush = brush)
        }

        item {
            SkeletonSectionHeader(brush = brush)
        }

        items(4) {
            SkeletonStockCard(brush = brush)
        }
    }
}

@Composable
private fun SkeletonPortfolioSummary(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(start = 24.dp, end = 14.dp, top = 14.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Currency toggle area (top-right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ShimmerBox(brush = brush, width = 80.dp, height = 24.dp, cornerRadius = 4.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total value + badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(brush = brush, width = 200.dp, height = 40.dp, cornerRadius = 8.dp)
                ShimmerBox(brush = brush, width = 60.dp, height = 28.dp, cornerRadius = 8.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info cards row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(brush = brush, width = 0.dp, height = 52.dp, cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f))
                ShimmerBox(brush = brush, width = 0.dp, height = 52.dp, cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f))
                ShimmerBox(brush = brush, width = 0.dp, height = 52.dp, cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sparkline area
            ShimmerBox(brush = brush, width = 0.dp, height = 100.dp, cornerRadius = 8.dp,
                modifier = Modifier.fillMaxWidth(0.9f))

            Spacer(modifier = Modifier.height(12.dp))

            // Period selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) {
                    ShimmerBox(brush = brush, width = 48.dp, height = 36.dp, cornerRadius = 8.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonSectionHeader(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(brush = brush, width = 100.dp, height = 20.dp)
        ShimmerBox(brush = brush, width = 60.dp, height = 16.dp)
    }
}

@Composable
private fun SkeletonStockCard(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Heatmap indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(brush)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock info (name, shares, weight)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(brush = brush, width = 120.dp, height = 18.dp)
                    ShimmerBox(brush = brush, width = 100.dp, height = 12.dp)
                    ShimmerBox(brush = brush, width = 80.dp, height = 12.dp)
                }

                // Value and change (right side)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(brush = brush, width = 70.dp, height = 18.dp)
                    ShimmerBox(brush = brush, width = 90.dp, height = 12.dp)
                    ShimmerBox(brush = brush, width = 60.dp, height = 24.dp, cornerRadius = 8.dp)
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(
    brush: Brush,
    height: Dp,
    width: Dp = 0.dp,
    cornerRadius: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (width > 0.dp) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}
