package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.domain.service.HouseholdCodeGenerator
import com.portfolio.manager.domain.service.SnapshotMapper
import com.portfolio.manager.util.ErrorMessages
import com.portfolio.manager.util.PreferenceKeys

/**
 * State for the household pairing screen.
 */
data class HouseholdShareUiState(
    val householdCode: String? = null,
    val myLabel: String = "",
    val isBusy: Boolean = false,
    val error: String? = null
)

/**
 * Manages household pairing: create a code or join an existing one, set the
 * display label, and publish my portfolio snapshot. Leaving clears the local
 * household config.
 */
@HiltViewModel
class HouseholdShareViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    private val cashRepository: CashRepository,
    private val snapshotMapper: SnapshotMapper,
    private val codeGenerator: HouseholdCodeGenerator,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitial())
    val uiState: StateFlow<HouseholdShareUiState> = _uiState.asStateFlow()

    private fun loadInitial() = HouseholdShareUiState(
        householdCode = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null),
        myLabel = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_MY_LABEL, null) ?: ""
    )

    fun createHousehold(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = ErrorMessages.HOUSEHOLD_LABEL_REQUIRED)
            return
        }
        pairWith(codeGenerator.generate(), trimmed)
    }

    fun joinHousehold(rawCode: String, label: String) {
        val code = codeGenerator.normalize(rawCode)
        val trimmed = label.trim()
        if (!codeGenerator.isValid(code)) {
            _uiState.value = _uiState.value.copy(error = ErrorMessages.HOUSEHOLD_CODE_INVALID)
            return
        }
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = ErrorMessages.HOUSEHOLD_LABEL_REQUIRED)
            return
        }
        pairWith(code, trimmed)
    }

    fun leave() {
        sharedPreferences.edit()
            .remove(PreferenceKeys.HOUSEHOLD_CODE)
            .remove(PreferenceKeys.HOUSEHOLD_MY_UID)
            .remove(PreferenceKeys.HOUSEHOLD_MY_LABEL)
            .remove(PreferenceKeys.HOUSEHOLD_PARTNER_SYMBOLS_JSON)
            .apply()
        _uiState.value = HouseholdShareUiState()
    }

    private fun pairWith(code: String, label: String) {
        _uiState.value = _uiState.value.copy(isBusy = true, error = null)
        viewModelScope.launch {
            val uid = syncRepository.ensureSignedIn().getOrElse {
                _uiState.value = _uiState.value.copy(isBusy = false, error = ErrorMessages.HOUSEHOLD_SIGN_IN_FAILED)
                return@launch
            }
            sharedPreferences.edit()
                .putString(PreferenceKeys.HOUSEHOLD_CODE, code)
                .putString(PreferenceKeys.HOUSEHOLD_MY_UID, uid)
                .putString(PreferenceKeys.HOUSEHOLD_MY_LABEL, label)
                .apply()
            val publishError = publishSnapshot(code, uid, label)
            _uiState.value = _uiState.value.copy(
                householdCode = code,
                myLabel = label,
                isBusy = false,
                error = publishError
            )
        }
    }

    private suspend fun publishSnapshot(code: String, uid: String, label: String): String? {
        val holdings = holdingsRepository.getAllHoldings().first()
        val accounts = accountRepository.getAllAccounts().first()
        val cash = cashRepository.getAllCashItems().first()
        val snapshot = snapshotMapper.toSnapshot(label, System.currentTimeMillis(), accounts, holdings, cash)
        return syncRepository.publishSnapshot(code, uid, snapshot).fold(
            onSuccess = { null },
            onFailure = { ErrorMessages.HOUSEHOLD_PUBLISH_FAILED }
        )
    }
}
