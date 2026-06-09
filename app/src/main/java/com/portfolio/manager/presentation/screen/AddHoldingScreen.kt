package com.portfolio.manager.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch

import com.portfolio.manager.presentation.component.form.AccountSelectionSection
import com.portfolio.manager.presentation.component.form.CurrencySegmentedButton
import com.portfolio.manager.presentation.component.form.FormScaffold
import com.portfolio.manager.presentation.theme.LocalAppStrings
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel

@Composable
fun AddHoldingScreen(
    viewModel: AddHoldingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    FormScaffold(
        title = if (uiState.isEditMode) strings.editHolding else strings.addHolding,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.needsAccountSelection) {
                Text(
                    text = strings.account,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                AccountSelectionSection(
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    onAccountSelected = { viewModel.selectAccount(it) }
                )
            }

            OutlinedTextField(
                value = uiState.symbol,
                onValueChange = { viewModel.updateSymbol(it) },
                label = { Text(strings.symbol) },
                placeholder = { Text(strings.symbolPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            viewModel.onSymbolFocusLost()
                        }
                    },
                singleLine = true,
                enabled = !uiState.isEditMode,
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = uiState.quantity,
                onValueChange = { viewModel.updateQuantity(it) },
                label = { Text(strings.quantity) },
                placeholder = { Text(strings.quantityPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.averagePrice,
                onValueChange = { viewModel.updateAveragePrice(it) },
                label = { Text(strings.averagePrice) },
                placeholder = { Text(strings.averagePricePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text(
                text = strings.currency,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            CurrencySegmentedButton(
                selectedCurrency = uiState.currency,
                onCurrencySelected = { viewModel.updateCurrency(it) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.saveHolding()) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isEditMode) strings.update else strings.addHolding)
            }
        }
    }
}
