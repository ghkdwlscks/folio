package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.portfolio.manager.presentation.theme.AppAnimations
import com.portfolio.manager.presentation.util.rememberHapticFeedback

/**
 * Modern Material 3 segmented button style currency toggle.
 * Features pill-shaped segments with currency icons and smooth animated indicator.
 */
@Composable
fun CurrencyToggle(
    showInKrw: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    // Track segment size for indicator animation
    val segmentWidth = 56.dp
    val segmentHeight = 24.dp
    val cornerRadius = 12.dp

    val indicatorOffset by animateDpAsState(
        targetValue = if (showInKrw) segmentWidth else 0.dp,
        animationSpec = AppAnimations.DpSprings.Toggle,
        label = "currencyToggleIndicator"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.12f))
            .height(segmentHeight)
    ) {
        // Animated indicator pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(segmentHeight)
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )

        // Segment buttons
        Row(
            modifier = Modifier.height(segmentHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // USD segment
            SegmentButton(
                text = "$ USD",
                isSelected = !showInKrw,
                onClick = {
                    if (showInKrw) {
                        haptic.tick()
                        onToggle()
                    }
                },
                modifier = Modifier
                    .width(segmentWidth)
                    .height(segmentHeight)
            )
            // KRW segment
            SegmentButton(
                text = "₩ KRW",
                isSelected = showInKrw,
                onClick = {
                    if (!showInKrw) {
                        haptic.tick()
                        onToggle()
                    }
                },
                modifier = Modifier
                    .width(segmentWidth)
                    .height(segmentHeight)
            )
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AutoSizeText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}
