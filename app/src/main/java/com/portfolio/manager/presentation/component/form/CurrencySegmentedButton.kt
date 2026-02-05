package com.portfolio.manager.presentation.component.form

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.presentation.theme.AppAnimations
import com.portfolio.manager.presentation.util.rememberHapticFeedback

/**
 * Material 3 style segmented button for currency selection (USD/KRW).
 * Extracted from AddHoldingScreen and AddCashScreen to eliminate duplication.
 */
@Composable
fun CurrencySegmentedButton(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val segmentWidth = 100.dp
    val segmentHeight = 40.dp
    val cornerRadius = 12.dp

    val indicatorOffset by animateDpAsState(
        targetValue = if (selectedCurrency.isKrw) segmentWidth else 0.dp,
        animationSpec = AppAnimations.DpSprings.Toggle,
        label = "currencyIndicator"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(cornerRadius)
            )
            .height(segmentHeight)
    ) {
        // Animated indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(segmentHeight)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(cornerRadius)
                )
        )

        // Segment buttons
        Row {
            CurrencySegment(
                text = "$ USD",
                isSelected = selectedCurrency.isUsd,
                onClick = {
                    if (!selectedCurrency.isUsd) {
                        haptic.tick()
                        onCurrencySelected(Currency.USD)
                    }
                },
                modifier = Modifier.width(segmentWidth)
            )
            CurrencySegment(
                text = "₩ KRW",
                isSelected = selectedCurrency.isKrw,
                onClick = {
                    if (!selectedCurrency.isKrw) {
                        haptic.tick()
                        onCurrencySelected(Currency.KRW)
                    }
                },
                modifier = Modifier.width(segmentWidth)
            )
        }
    }
}

@Composable
private fun CurrencySegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
