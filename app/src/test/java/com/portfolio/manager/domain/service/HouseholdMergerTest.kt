package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.model.SnapshotAccount
import com.portfolio.manager.domain.model.SnapshotCashItem
import com.portfolio.manager.domain.model.SnapshotHolding
import org.junit.Test

class HouseholdMergerTest {

    private val merger = HouseholdMerger()

    private val myAccounts = listOf(AccountEntity(id = 1L, name = "Mine", orderIndex = 0))
    private val myHoldings = listOf(
        HoldingEntity(id = 1L, accountId = 1L, symbol = "AAPL", name = "Apple",
            quantity = 5, averagePrice = 150.0, currency = "USD")
    )
    private val myCash = listOf(
        CashItemEntity(id = 1L, accountId = 1L, name = "MyCash",
            originalValue = 100.0, annualYieldRate = 1.0, currency = "USD")
    )

    private val partner = PortfolioSnapshot(
        displayName = "와이프",
        accounts = listOf(SnapshotAccount(localId = 1L, name = "HerAcct", order = 0)),
        holdings = listOf(
            SnapshotHolding(localId = 1L, accountLocalId = 1L, symbol = "GOOGL",
                name = "Alphabet", quantity = 3, averagePrice = 100.0, currency = Currency.USD)
        ),
        cashItems = listOf(
            SnapshotCashItem(localId = 1L, accountLocalId = 1L, name = "HerCash",
                value = 200.0, annualYieldRate = 2.0, currency = Currency.KRW)
        )
    )

    @Test
    fun `merge - no partner returns only my data`() {
        val result = merger.merge(myAccounts, myHoldings, myCash, "나", partnerSnapshot = null)

        assertThat(result.accounts).hasSize(1)
        assertThat(result.holdings).hasSize(1)
        assertThat(result.cashItems).hasSize(1)
        assertThat(result.partnerAccountIds).isEmpty()
        assertThat(result.ownerLabels[1L]).isEqualTo("나")
    }

    @Test
    fun `merge - combines my data and partner with namespaced ids`() {
        val result = merger.merge(myAccounts, myHoldings, myCash, "나", partner)

        assertThat(result.accounts).hasSize(2)
        assertThat(result.holdings).hasSize(2)
        assertThat(result.cashItems).hasSize(2)

        // Partner account id is namespaced to negative space, distinct from mine (1L).
        val partnerAccount = result.accounts.first { it.id < 0 }
        assertThat(partnerAccount.name).isEqualTo("HerAcct")
        assertThat(result.partnerAccountIds).containsExactly(partnerAccount.id)

        // Partner holding points at the namespaced account id.
        val partnerHolding = result.holdings.first { it.symbol == "GOOGL" }
        assertThat(partnerHolding.accountId).isEqualTo(partnerAccount.id)
        assertThat(partnerHolding.id).isLessThan(0L)

        // Partner cash points at the namespaced account id.
        val partnerCash = result.cashItems.first { it.name == "HerCash" }
        assertThat(partnerCash.accountId).isEqualTo(partnerAccount.id)

        // Owner labels cover both owners.
        assertThat(result.ownerLabels[1L]).isEqualTo("나")
        assertThat(result.ownerLabels[partnerAccount.id]).isEqualTo("와이프")
    }

    @Test
    fun `merge - partner currency enum maps back to entity code`() {
        val result = merger.merge(myAccounts, myHoldings, myCash, "나", partner)

        assertThat(result.cashItems.first { it.name == "HerCash" }.currency).isEqualTo("KRW")
        assertThat(result.holdings.first { it.symbol == "GOOGL" }.currency).isEqualTo("USD")
    }
}
