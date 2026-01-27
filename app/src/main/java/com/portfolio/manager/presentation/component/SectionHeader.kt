package com.portfolio.manager.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.TimePeriod

/**
 * A header component for list sections with title, count, and filter dropdowns.
 * Used to display holdings section with sort and sparkline period options.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int,
    sortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    sparklinePeriod: TimePeriod,
    onSparklinePeriodSelected: (TimePeriod) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sparklineMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort dropdown
            SortDropdown(
                sortOption = sortOption,
                expanded = sortMenuExpanded,
                onExpandedChange = { sortMenuExpanded = it },
                onSortOptionSelected = {
                    onSortOptionSelected(it)
                    sortMenuExpanded = false
                }
            )
            // Sparkline period dropdown
            SparklinePeriodDropdown(
                sparklinePeriod = sparklinePeriod,
                expanded = sparklineMenuExpanded,
                onExpandedChange = { sparklineMenuExpanded = it },
                onPeriodSelected = {
                    onSparklinePeriodSelected(it)
                    sparklineMenuExpanded = false
                }
            )
        }
    }
}

@Composable
private fun SortDropdown(
    sortOption: SortOption,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortOptionSelected: (SortOption) -> Unit
) {
    Box {
        Row(
            modifier = Modifier.clickable { onExpandedChange(true) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Sort",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = sortOption.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun SparklinePeriodDropdown(
    sparklinePeriod: TimePeriod,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Box {
        Row(
            modifier = Modifier.clickable { onExpandedChange(true) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ShowChart,
                contentDescription = "Sparkline Period",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = sparklinePeriod.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = period.label,
                            fontWeight = if (period == sparklinePeriod) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onPeriodSelected(period) }
                )
            }
        }
    }
}
