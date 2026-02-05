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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch

import com.portfolio.manager.presentation.component.form.AccountSelectionSection
import com.portfolio.manager.presentation.component.form.CurrencySegmentedButton
import com.portfolio.manager.presentation.component.form.FormScaffold
import com.portfolio.manager.presentation.viewmodel.AddCashViewModel

@Composable
fun AddCashScreen(
    viewModel: AddCashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    FormScaffold(
        title = if (uiState.isEditMode) "Edit Cash" else "Add Cash",
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
                    text = "Account",
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
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name") },
                placeholder = { Text("e.g., Emergency Fund, CD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = uiState.value,
                onValueChange = { viewModel.updateValue(it) },
                label = { Text("Value") },
                placeholder = { Text("e.g., 10000") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.yieldRate,
                onValueChange = { viewModel.updateYieldRate(it) },
                label = { Text("Annual Yield (%)") },
                placeholder = { Text("e.g., 4.5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text(
                text = "Currency",
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
                        if (viewModel.saveCashItem()) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isEditMode) "Update" else "Add Cash")
            }
        }
    }
}
