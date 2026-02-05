package com.portfolio.manager.presentation.viewmodel.base

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.model.Currency

/**
 * Common interface for form UI states (AddHolding, AddCash).
 * Provides type safety for shared form components.
 */
interface FormUiState {
    val isInitialLoading: Boolean
    val currency: Currency
    val errorMessage: String?
    val selectedAccountId: Long
    val accounts: List<AccountEntity>
    val needsAccountSelection: Boolean
    val isEditMode: Boolean
    val isSaving: Boolean
}
