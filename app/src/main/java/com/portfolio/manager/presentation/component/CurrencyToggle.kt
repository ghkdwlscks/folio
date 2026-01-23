package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
fun CurrencyToggle(
    showInKrw: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorOffset by animateFloatAsState(
        targetValue = if (showInKrw) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "currencyToggleIndicator"
    )

    Layout(
        content = {
            // Indicator (measured but positioned manually)
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            // USD option
            Text(
                text = "USD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (!showInKrw) FontWeight.Bold else FontWeight.Normal,
                color = if (!showInKrw) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            // KRW option
            Text(
                text = "KRW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (showInKrw) FontWeight.Bold else FontWeight.Normal,
                color = if (showInKrw) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onToggle)
    ) { measurables, constraints ->
        val usdPlaceable = measurables[1].measure(constraints)
        val krwPlaceable = measurables[2].measure(constraints)

        val width = usdPlaceable.width + krwPlaceable.width
        val height = maxOf(usdPlaceable.height, krwPlaceable.height)

        val indicatorWidth = if (showInKrw) krwPlaceable.width else usdPlaceable.width
        val indicatorX = (usdPlaceable.width * indicatorOffset).toInt()
        val indicatorPlaceable = measurables[0].measure(
            Constraints.fixed(indicatorWidth, height)
        )

        layout(width, height) {
            indicatorPlaceable.placeRelative(indicatorX, 0)
            usdPlaceable.placeRelative(0, 0)
            krwPlaceable.placeRelative(usdPlaceable.width, 0)
        }
    }
}
