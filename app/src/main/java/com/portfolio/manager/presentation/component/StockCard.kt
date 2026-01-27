package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.LossRed
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.createTrendIndicator
import com.portfolio.manager.presentation.util.getTrendColor
import kotlin.math.abs
import kotlin.math.min

@Composable
fun StockCard(
    stock: Stock,
    weightPercent: Double? = null,
    targetWeight: Int? = null,
    onDelete: (() -> Unit)? = null,
    onDeleteAccountHolding: ((Long) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onEditAccountHolding: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val trend = createTrendIndicator(stock.gainLoss)
    var expanded by remember { mutableStateOf(false) }
    var showFullScreenChart by remember { mutableStateOf(false) }
    val hasAccountDetails = stock.accountDetails.isNotEmpty()
    val canExpand = hasAccountDetails || onDelete != null || onEdit != null

    // Scale animation on press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "cardScale"
    )

    // Heatmap: color intensity based on gain/loss percentage (max at 50%)
    val heatmapIntensity = min(abs(stock.gainLossPercent) / 50.0, 1.0).toFloat()
    val heatmapColor = getTrendColor(stock.gainLoss)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Heatmap indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(heatmapColor.copy(alpha = 0.3f + (heatmapIntensity * 0.7f)))
            )
            Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .then(
                        if (canExpand) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { expanded = !expanded }
                        } else {
                            Modifier
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock info with sparkline background
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { showFullScreenChart = true },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Sparkline as background (if available)
                    if (stock.priceHistory.size >= 2) {
                        Sparkline(
                            prices = stock.priceHistory,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.5f },
                            onClick = null
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Row 1: Stock name (can wrap to 2 lines if needed)
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        // Row 2: shares info + dividend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stock.quantity} shares @ ${CurrencyFormatter.format(stock.averagePrice, stock.currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (stock.dividendYield != null && stock.dividendYield > 0) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Div ${CurrencyFormatter.formatPercent(stock.dividendYield)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        // Row 3: weight
                        if (weightPercent != null) {
                            val weightText = if (targetWeight != null) {
                                "Weight ${CurrencyFormatter.formatPercent(weightPercent)}% / $targetWeight%"
                            } else {
                                "Weight ${CurrencyFormatter.formatPercent(weightPercent)}%"
                            }
                            Text(
                                text = weightText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } // Column (stock text)
                } // Box (sparkline wrapper)

                // Value and change
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.format(stock.currentPrice, stock.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total ${CurrencyFormatter.format(stock.totalValue, stock.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Day change row (compact: icon + percentage only)
                    if (stock.dayChange != null && stock.dayChangePercent != null) {
                        val dayTrend = createTrendIndicator(stock.dayChange)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = dayTrend.icon,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = dayTrend.color
                            )
                            Text(
                                text = "${if (dayTrend.isGain) "+" else ""}${CurrencyFormatter.formatPercent(stock.dayChangePercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = dayTrend.color
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = trend.backgroundColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = trend.icon,
                                    contentDescription = if (trend.isGain) "Trending up" else "Trending down",
                                    modifier = Modifier.size(14.dp),
                                    tint = trend.color
                                )
                                Text(
                                    text = "${if (trend.isGain) "+" else ""}${CurrencyFormatter.formatPercent(stock.gainLossPercent)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = trend.color
                                )
                            }
                        }
                        if (canExpand) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expandable section
            AnimatedVisibility(
                visible = expanded && canExpand,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                StockCardExpandableContent(
                    stock = stock,
                    hasAccountDetails = hasAccountDetails,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onEditAccountHolding = onEditAccountHolding,
                    onDeleteAccountHolding = onDeleteAccountHolding
                )
            }
            } // Column (card content)
        } // Row (heatmap + content)
    } // Card

    // Full screen chart dialog
    if (showFullScreenChart) {
        FullScreenChartDialog(
            data = FullScreenChartData(
                symbol = stock.symbol,
                name = stock.name,
                currentPrice = stock.currentPrice,
                dayChange = stock.dayChange,
                dayChangePercent = stock.dayChangePercent,
                currency = stock.currency,
                priceHistory = stock.priceHistory,
                priceHistoryTimestamps = stock.priceHistoryTimestamps
            ),
            onDismiss = { showFullScreenChart = false }
        )
    }
}

@Composable
private fun StockCardExpandableContent(
    stock: Stock,
    hasAccountDetails: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onEditAccountHolding: ((Long) -> Unit)?,
    onDeleteAccountHolding: ((Long) -> Unit)?
) {
    Column {
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        if (hasAccountDetails) {
            AccountDetailsSection(
                accountDetails = stock.accountDetails,
                currency = stock.currency,
                onEditAccountHolding = onEditAccountHolding,
                onDeleteAccountHolding = onDeleteAccountHolding
            )
        }

        if (onEdit != null || onDelete != null) {
            ActionButtonsRow(
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun AccountDetailsSection(
    accountDetails: List<com.portfolio.manager.domain.model.StockAccountDetail>,
    currency: String,
    onEditAccountHolding: ((Long) -> Unit)?,
    onDeleteAccountHolding: ((Long) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Per Account",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        accountDetails.forEach { detail ->
            AccountDetailRow(
                detail = detail,
                currency = currency,
                onEdit = onEditAccountHolding?.let { { it(detail.holdingId) } },
                onDelete = onDeleteAccountHolding?.let { { it(detail.holdingId) } }
            )
        }
    }
}

@Composable
private fun AccountDetailRow(
    detail: com.portfolio.manager.domain.model.StockAccountDetail,
    currency: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = detail.accountName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${detail.quantity} @ ${CurrencyFormatter.format(detail.averagePrice, currency)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit in ${detail.accountName}",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete from ${detail.accountName}",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
