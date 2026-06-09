package com.portfolio.manager.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.portfolio.manager.domain.model.FIRECalculation
import com.portfolio.manager.domain.model.FIRETargetCalculation
import com.portfolio.manager.presentation.component.CurrencyToggle
import com.portfolio.manager.presentation.component.ErrorContent
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.LocalAppStrings
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.viewmodel.FIRECalculatorUiState
import com.portfolio.manager.presentation.viewmodel.FIRECalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FIRECalculatorScreen(
    viewModel: FIRECalculatorViewModel,
    onNavigateBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                title = { Text(strings.fireCalculator, fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState is FIRECalculatorUiState.Success) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.refresh)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is FIRECalculatorUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FIRECalculatorUiState.Success -> {
                FIRECalculatorContent(
                    state = state,
                    onAnnualReturnChange = { viewModel.updateAnnualReturn(it) },
                    onAnnualInflationChange = { viewModel.updateAnnualInflation(it) },
                    onTargetSpendingChange = { viewModel.updateTargetMonthlySpending(it) },
                    onCurrencyToggle = { viewModel.toggleCurrency() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is FIRECalculatorUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun FIRECalculatorContent(
    state: FIRECalculatorUiState.Success,
    onAnnualReturnChange: (Double) -> Unit,
    onAnnualInflationChange: (Double) -> Unit,
    onTargetSpendingChange: (Double) -> Unit,
    onCurrencyToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatValue = CurrencyFormatter.createFormatter(state.showInKrw)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Portfolio Summary Card
        PortfolioSummaryCard(
            totalValue = state.totalPortfolioValue,
            showInKrw = state.showInKrw,
            onCurrencyToggle = onCurrencyToggle,
            formatValue = formatValue
        )

        // Settings Card
        SettingsCard(
            annualReturn = state.annualReturn,
            annualInflation = state.annualInflation,
            onAnnualReturnChange = onAnnualReturnChange,
            onAnnualInflationChange = onAnnualInflationChange
        )

        // Sustainable Spending Card
        SustainableSpendingCard(
            calculation = state.fireCalculation,
            formatValue = formatValue
        )

        // Target FIRE Card
        TargetFIRECard(
            calculation = state.fireTargetCalculation,
            showInKrw = state.showInKrw,
            onTargetSpendingChange = onTargetSpendingChange,
            formatValue = formatValue
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PortfolioSummaryCard(
    totalValue: Double,
    showInKrw: Boolean,
    onCurrencyToggle: () -> Unit,
    formatValue: (Double) -> String
) {
    val strings = LocalAppStrings.current
    val isDarkTheme = isSystemInDarkTheme()
    val gradientColors = if (isDarkTheme) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors
                )
            )
    ) {
        CurrencyToggle(
            showInKrw = showInKrw,
            onToggle = onCurrencyToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.totalPortfolio,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatValue(totalValue),
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = (-1).sp
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SettingsCard(
    annualReturn: Double,
    annualInflation: Double,
    onAnnualReturnChange: (Double) -> Unit,
    onAnnualInflationChange: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var returnText by remember(annualReturn) { mutableStateOf(annualReturn.toString()) }
    var inflationText by remember(annualInflation) { mutableStateOf(annualInflation.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.settings,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = returnText,
                    onValueChange = { newValue ->
                        returnText = newValue
                        newValue.toDoubleOrNull()?.let { onAnnualReturnChange(it) }
                    },
                    label = { Text(strings.annualReturn) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = inflationText,
                    onValueChange = { newValue ->
                        inflationText = newValue
                        newValue.toDoubleOrNull()?.let { onAnnualInflationChange(it) }
                    },
                    label = { Text(strings.annualInflation) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            val realReturn = annualReturn - annualInflation
            Text(
                text = "${strings.realReturn}: ${String.format("%.1f", realReturn)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = if (realReturn >= 0) GainGreen else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SustainableSpendingCard(
    calculation: FIRECalculation,
    formatValue: (Double) -> String
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.sustainableSpending,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = strings.sustainableSpendingDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpendingItem(
                    label = strings.monthly,
                    value = formatValue(calculation.sustainableMonthlySpending)
                )
                SpendingItem(
                    label = strings.annually,
                    value = formatValue(calculation.sustainableAnnualSpending)
                )
            }

            if (calculation.realReturn <= 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.realReturnWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SpendingItem(
    label: String,
    value: String
) {
    val strings = LocalAppStrings.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GainGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TargetFIRECard(
    calculation: FIRETargetCalculation,
    showInKrw: Boolean,
    onTargetSpendingChange: (Double) -> Unit,
    formatValue: (Double) -> String
) {
    val strings = LocalAppStrings.current
    var targetText by remember(calculation.targetMonthlySpending) {
        mutableStateOf(calculation.targetMonthlySpending.toLong().toString())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.fireTarget,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = targetText,
                onValueChange = { newValue ->
                    targetText = newValue
                    newValue.toDoubleOrNull()?.let { onTargetSpendingChange(it) }
                },
                label = { Text(strings.targetMonthlySpendingLabel(showInKrw)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = strings.requiredPortfolio,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (calculation.requiredPortfolio.isFinite()) {
                    formatValue(calculation.requiredPortfolio)
                } else {
                    strings.notAvailableRealReturn
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.progress,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${String.format("%.1f", calculation.progressPercent)}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GainGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (calculation.progressPercent / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GainGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (calculation.remainingAmount > 0 && calculation.requiredPortfolio.isFinite()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.remaining}: ${formatValue(calculation.remainingAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
