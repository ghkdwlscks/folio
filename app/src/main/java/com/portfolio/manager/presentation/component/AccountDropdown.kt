package com.portfolio.manager.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.IconButton
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
import com.portfolio.manager.presentation.viewmodel.AccountWithCount
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID

/**
 * A dropdown component for selecting portfolio accounts.
 * Shows the selected account name with holdings count, and a dropdown menu
 * to switch between accounts or view all.
 */
@Composable
fun AccountDropdown(
    accounts: List<AccountWithCount>,
    selectedAccountId: Long,
    filteredAccountIds: Set<Long>,
    onAccountSelected: (Long) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val totalHoldings = accounts.sumOf { it.holdingsCount }
    val isFilterActive = filteredAccountIds.isNotEmpty()
    val filteredCount = if (isFilterActive) {
        accounts.filter { it.account.id in filteredAccountIds }.sumOf { it.holdingsCount }
    } else {
        totalHoldings
    }

    val selectedLabel = when {
        selectedAccountId != ALL_ACCOUNTS_ID -> {
            accounts.find { it.account.id == selectedAccountId }?.let {
                "${it.account.name} (${it.holdingsCount})"
            } ?: "All ($totalHoldings)"
        }
        isFilterActive -> "${filteredAccountIds.size} of ${accounts.size} ($filteredCount)"
        else -> "All ($totalHoldings)"
    }

    val anyNeedsRebalance = accounts.any { it.needsRebalance }
    val showFilterIcon = selectedAccountId == ALL_ACCOUNTS_ID

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedAccountId == ALL_ACCOUNTS_ID && anyNeedsRebalance) {
                    Icon(
                        imageVector = Icons.Outlined.Balance,
                        contentDescription = "Needs rebalancing",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Select account",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "All ($totalHoldings)",
                            fontWeight = if (selectedAccountId == ALL_ACCOUNTS_ID) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onAccountSelected(ALL_ACCOUNTS_ID)
                        expanded = false
                    }
                )
                accounts.forEach { accountWithCount ->
                    val isSelected = selectedAccountId == accountWithCount.account.id
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${accountWithCount.account.name} (${accountWithCount.holdingsCount})",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (accountWithCount.needsRebalance) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Balance,
                                        contentDescription = "Needs rebalancing",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        onClick = {
                            onAccountSelected(accountWithCount.account.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (showFilterIcon) {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter accounts",
                    modifier = Modifier.size(20.dp),
                    tint = if (isFilterActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}
