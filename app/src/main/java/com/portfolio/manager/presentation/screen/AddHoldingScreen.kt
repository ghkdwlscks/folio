package com.portfolio.manager.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHoldingScreen(
    viewModel: AddHoldingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Holding" else "Add Holding") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.needsAccountSelection) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { account ->
                        FilterChip(
                            selected = uiState.selectedAccountId == account.id,
                            onClick = { viewModel.selectAccount(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.symbol,
                onValueChange = { viewModel.updateSymbol(it) },
                label = { Text("Symbol") },
                placeholder = { Text("e.g., AAPL or 005930") },
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
                label = { Text("Quantity") },
                placeholder = { Text("e.g., 10") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.averagePrice,
                onValueChange = { viewModel.updateAveragePrice(it) },
                label = { Text("Average Price") },
                placeholder = { Text("e.g., 150.00") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.currency == "USD",
                    onClick = { viewModel.updateCurrency("USD") },
                    label = { Text("USD ($)") }
                )
                FilterChip(
                    selected = uiState.currency == "KRW",
                    onClick = { viewModel.updateCurrency("KRW") },
                    label = { Text("KRW (₩)") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.saveHolding()) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isEditMode) "Update" else "Add Holding")
            }
        }
    }
}
