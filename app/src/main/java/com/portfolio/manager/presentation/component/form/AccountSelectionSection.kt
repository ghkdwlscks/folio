package com.portfolio.manager.presentation.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.presentation.util.rememberHapticFeedback

/**
 * Reusable account selection section with filter chips.
 * Extracted from AddHoldingScreen and AddCashScreen to eliminate duplication.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSelectionSection(
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { account ->
            FilterChip(
                selected = selectedAccountId == account.id,
                onClick = {
                    haptic.tick()
                    onAccountSelected(account.id)
                },
                label = { Text(account.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
