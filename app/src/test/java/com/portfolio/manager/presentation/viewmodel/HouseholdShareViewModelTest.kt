package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.domain.service.HouseholdCodeGenerator
import com.portfolio.manager.domain.service.SnapshotMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdShareViewModelTest {

    private lateinit var syncRepository: SyncRepository
    private lateinit var holdingsRepository: HoldingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var cashRepository: CashRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val mapper = SnapshotMapper()
    private val codeGenerator = HouseholdCodeGenerator()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val prefValues = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        syncRepository = mockk()
        holdingsRepository = mockk()
        accountRepository = mockk()
        cashRepository = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            prefValues[firstArg()] = secondArg<String>()
            editor
        }
        every { editor.remove(any()) } answers {
            prefValues.remove(firstArg<String>())
            editor
        }
        every { sharedPreferences.getString(any(), null) } answers {
            prefValues[firstArg()] as? String
        }

        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { cashRepository.getAllCashItems() } returns flowOf(emptyList())

        coEvery { syncRepository.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { syncRepository.publishSnapshot(any(), any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HouseholdShareViewModel(
        syncRepository, holdingsRepository, accountRepository, cashRepository,
        mapper, codeGenerator, sharedPreferences
    )

    @Test
    fun `init - loads existing config from prefs`() {
        prefValues["household_code"] = "ABCD-2345"
        prefValues["household_my_label"] = "남편"

        val state = createViewModel().uiState.value

        assertThat(state.householdCode).isEqualTo("ABCD-2345")
        assertThat(state.myLabel).isEqualTo("남편")
    }

    @Test
    fun `createHousehold - generates code persists and publishes`() = runTest {
        val vm = createViewModel()

        vm.createHousehold("남편")

        val state = vm.uiState.value
        assertThat(state.householdCode).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}")
        assertThat(state.error).isNull()
        assertThat(prefValues["household_code"]).isEqualTo(state.householdCode)
        assertThat(prefValues["household_my_uid"]).isEqualTo("uid-1")
        assertThat(prefValues["household_my_label"]).isEqualTo("남편")
        coVerify { syncRepository.publishSnapshot(state.householdCode!!, "uid-1", any()) }
    }

    @Test
    fun `createHousehold - blank label - sets error and does not sign in`() = runTest {
        val vm = createViewModel()

        vm.createHousehold("   ")

        assertThat(vm.uiState.value.householdCode).isNull()
        assertThat(vm.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { syncRepository.ensureSignedIn() }
    }

    @Test
    fun `createHousehold - sign-in fails - sets error and stays unpaired`() = runTest {
        coEvery { syncRepository.ensureSignedIn() } returns Result.failure(RuntimeException("offline"))
        val vm = createViewModel()

        vm.createHousehold("남편")

        assertThat(vm.uiState.value.householdCode).isNull()
        assertThat(vm.uiState.value.error).isNotNull()
    }

    @Test
    fun `joinHousehold - valid code - normalizes persists and publishes`() = runTest {
        val vm = createViewModel()

        vm.joinHousehold("abcd2345", "와이프")

        val state = vm.uiState.value
        assertThat(state.householdCode).isEqualTo("ABCD-2345")
        assertThat(state.error).isNull()
        assertThat(prefValues["household_code"]).isEqualTo("ABCD-2345")
        coVerify { syncRepository.publishSnapshot("ABCD-2345", "uid-1", any()) }
    }

    @Test
    fun `joinHousehold - invalid code - sets error`() = runTest {
        val vm = createViewModel()

        vm.joinHousehold("nope", "와이프")

        assertThat(vm.uiState.value.householdCode).isNull()
        assertThat(vm.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { syncRepository.ensureSignedIn() }
    }

    @Test
    fun `joinHousehold - blank label - sets error`() = runTest {
        val vm = createViewModel()

        vm.joinHousehold("ABCD-2345", "")

        assertThat(vm.uiState.value.householdCode).isNull()
        assertThat(vm.uiState.value.error).isNotNull()
    }

    @Test
    fun `leave - clears config`() = runTest {
        prefValues["household_code"] = "ABCD-2345"
        prefValues["household_my_uid"] = "uid-1"
        prefValues["household_my_label"] = "남편"
        val vm = createViewModel()

        vm.leave()

        assertThat(vm.uiState.value.householdCode).isNull()
        assertThat(prefValues).doesNotContainKey("household_code")
        assertThat(prefValues).doesNotContainKey("household_my_uid")
    }

    @Test
    fun `createHousehold - publish fails - stays paired with error`() = runTest {
        coEvery { syncRepository.publishSnapshot(any(), any(), any()) } returns
            Result.failure(RuntimeException("write failed"))
        val vm = createViewModel()

        vm.createHousehold("남편")

        assertThat(vm.uiState.value.householdCode).isNotNull()
        assertThat(vm.uiState.value.error).isNotNull()
    }
}
