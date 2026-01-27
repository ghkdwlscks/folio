package com.portfolio.manager.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.util.CurrencyFormatter

@Composable
fun CashCard(
    cashItem: CashItem,
    showInKrw: Boolean,
    exchangeRate: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = if (showInKrw) {
        cashItem.valueInKrw(exchangeRate)
    } else {
        cashItem.valueInUsd(exchangeRate)
    }

    val shape = RoundedCornerShape(20.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Glass effect gradient
    val glassGradient = Brush.linearGradient(
        colors = listOf(
            surfaceColor.copy(alpha = 0.9f),
            surfaceColor.copy(alpha = 0.75f)
        )
    )
    val highlightGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glassGradient)
            .background(highlightGradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = cashItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = CurrencyFormatter.format(currentValue, if (showInKrw) "KRW" else "USD"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Yield: ${String.format("%.2f", cashItem.annualYieldRate)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = GainGreen
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    } // Box (glass card)
}
