package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.presentation.theme.AppAnimations
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.LossRed
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.rememberHapticFeedback

import kotlin.math.abs

data class RebalanceItem(
    val holdingId: Long,
    val symbol: String,
    val name: String,
    val currentValue: Double,
    val currentPrice: Double,
    val currentPercentage: Int?,
    val currency: Currency,
    val quantity: Int
)

data class RebalanceRecommendation(
    val holdingId: Long,
    val symbol: String,
    val name: String,
    val currentPercent: Double,
    val targetPercent: Double,
    val diffPercent: Double,
    val diffAmount: Double,
    val currentPrice: Double,
    val currency: Currency,
    val currentShares: Int,
    val idealShares: Double
)

@Composable
fun RebalanceDialog(
    items: List<RebalanceItem>,
    initialToleranceBandPercent: Int?,
    showInKrw: Boolean,
    onDismiss: () -> Unit,
    onSave: (percentages: Map<Long, Int?>, band: Int?) -> Unit,
    onReset: () -> Unit
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }

    val haptic = rememberHapticFeedback()

    var percentageTexts by remember {
        mutableStateOf(
            items.associate { it.holdingId to (it.currentPercentage?.toString() ?: "") }
        )
    }
    var bandText by remember {
        mutableStateOf(initialToleranceBandPercent?.toString().orEmpty())
    }
    val parsedBand: Int? = bandText.trim().toIntOrNull()?.coerceIn(1, 100)

    val parsedPercentages: Map<Long, Int?> = percentageTexts.mapValues { (_, text) ->
        text.trim().toIntOrNull()?.coerceIn(0, 100)
    }
    val totalPercentage = parsedPercentages.values.filterNotNull().sum()
    val recommendations = calculateRecommendations(items, parsedPercentages)

    BaseDialog(
        onDismiss = onDismiss,
        horizontalPadding = 16.dp,
        maxHeightFraction = 0.85f,
        enterTransition = fadeIn(tween(AppAnimations.Duration.FAST)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(AppAnimations.Duration.NORMAL)
                ),
        exitTransition = fadeOut(tween(AppAnimations.Duration.FAST)) +
                slideOutVertically(
                    targetOffsetY = { it / 4 },
                    animationSpec = tween(AppAnimations.Duration.FAST)
                )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Balance,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rebalance Portfolio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tolerance band input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tolerance band",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Empty = per-share rule",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = bandText,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }
                        bandText = when {
                            digits.isEmpty() -> ""
                            else -> digits.toIntOrNull()?.coerceIn(1, 100)?.toString().orEmpty()
                        }
                    },
                    modifier = Modifier.width(88.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("%") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Percentage input section
            Text(
                text = "Set Target Percentages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    PercentageInputRow(
                        symbol = item.symbol,
                        name = item.name,
                        text = percentageTexts[item.holdingId].orEmpty(),
                        isOverBudget = totalPercentage > 100,
                        onTextChange = { newText ->
                            percentageTexts = percentageTexts.toMutableMap().apply {
                                this[item.holdingId] = newText
                            }
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val badgeColor = when {
                            totalPercentage == 100 -> GainGreen
                            totalPercentage > 100 -> LossRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$totalPercentage%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (totalPercentage > 0 && recommendations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(recommendations) { rec ->
                        RecommendationRow(
                            recommendation = rec,
                            showInKrw = showInKrw
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.tick()
                        onReset()
                    },
                    enabled = items.any { it.currentPercentage != null } || initialToleranceBandPercent != null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset")
                }
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        haptic.tick()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        haptic.click()
                        onSave(parsedPercentages, parsedBand)
                    },
                    enabled = totalPercentage == 100,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun PercentageInputRow(
    symbol: String,
    name: String,
    text: String,
    isOverBudget: Boolean,
    onTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 18.sp
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }
                onTextChange(
                    when {
                        digits.isEmpty() -> ""
                        else -> digits.toIntOrNull()?.coerceIn(0, 100)?.toString().orEmpty()
                    }
                )
            },
            modifier = Modifier.width(72.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("%") },
            colors = if (isOverBudget) {
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LossRed,
                    unfocusedBorderColor = LossRed.copy(alpha = 0.5f)
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            }
        )
    }
}

@Composable
private fun RecommendationRow(
    recommendation: RebalanceRecommendation,
    showInKrw: Boolean
) {
    val isBuy = recommendation.diffAmount > 0
    val actionColor = if (isBuy) GainGreen else LossRed
    val actionText = if (isBuy) "Buy" else "Sell"
    val absAmount = abs(recommendation.diffAmount)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = recommendation.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
                Text(
                    text = "${CurrencyFormatter.formatPercent(recommendation.currentPercent)}% → ${CurrencyFormatter.formatPercent(recommendation.targetPercent)}% (${recommendation.currentShares} → ${"%.2f".format(recommendation.idealShares)} shares)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (absAmount >= recommendation.currentPrice && recommendation.currentPrice > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                        Text(
                            text = if (showInKrw) {
                                CurrencyFormatter.formatKrw(absAmount)
                            } else {
                                CurrencyFormatter.formatUsd(absAmount)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = actionColor
                        )
                    }
                    Icon(
                        imageVector = if (isBuy) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = actionText,
                        tint = actionColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun calculateRecommendations(
    items: List<RebalanceItem>,
    percentages: Map<Long, Int?>
): List<RebalanceRecommendation> {
    val totalPercentage = percentages.values.filterNotNull().sum()
    if (totalPercentage <= 0) return emptyList()

    val totalCurrentValue = items.sumOf { item ->
        if (percentages[item.holdingId] == null) 0.0 else item.currentValue
    }
    if (totalCurrentValue <= 0) return emptyList()

    return items.mapNotNull { item ->
        val target = percentages[item.holdingId] ?: return@mapNotNull null
        val currentPercent = (item.currentValue / totalCurrentValue) * 100

        val targetPercent = target.toDouble()

        val diffPercent = targetPercent - currentPercent
        val targetValue = totalCurrentValue * (targetPercent / 100)
        val diffAmount = targetValue - item.currentValue

        val idealShares = if (item.currentPrice > 0) {
            targetValue / item.currentPrice
        } else 0.0

        RebalanceRecommendation(
            holdingId = item.holdingId,
            symbol = item.symbol,
            name = item.name,
            currentPercent = currentPercent,
            targetPercent = targetPercent,
            diffPercent = diffPercent,
            diffAmount = diffAmount,
            currentPrice = item.currentPrice,
            currency = item.currency,
            currentShares = item.quantity,
            idealShares = idealShares
        )
    }.sortedByDescending { abs(it.diffAmount) }
}
