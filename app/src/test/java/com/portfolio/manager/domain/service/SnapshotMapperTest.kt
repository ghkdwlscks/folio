package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.SnapshotAccount
import org.junit.Test

class SnapshotMapperTest {

    private val mapper = SnapshotMapper()

    @Test
    fun `toSnapshot - maps accounts holdings and cash`() {
        val snapshot = mapper.toSnapshot(
            displayName = "남편",
            updatedAt = 42L,
            accounts = listOf(AccountEntity(id = 1L, name = "Retirement", orderIndex = 2)),
            holdings = listOf(
                HoldingEntity(
                    id = 10L, accountId = 1L, symbol = "AAPL", name = "Apple Inc.",
                    quantity = 5, averagePrice = 150.0, currency = "USD", targetPercentage = 20
                )
            ),
            cashItems = listOf(
                CashItemEntity(
                    id = 100L, accountId = 1L, name = "Savings",
                    originalValue = 1000.0, annualYieldRate = 3.5, currency = "KRW"
                )
            )
        )

        assertThat(snapshot.displayName).isEqualTo("남편")
        assertThat(snapshot.updatedAt).isEqualTo(42L)
        assertThat(snapshot.accounts).containsExactly(
            SnapshotAccount(localId = 1L, name = "Retirement", order = 2)
        )
        val h = snapshot.holdings.single()
        assertThat(h.symbol).isEqualTo("AAPL")
        assertThat(h.name).isEqualTo("Apple Inc.")
        assertThat(h.quantity).isEqualTo(5)
        assertThat(h.currency).isEqualTo(Currency.USD)
        assertThat(h.targetPercentage).isEqualTo(20)
        val c = snapshot.cashItems.single()
        assertThat(c.value).isEqualTo(1000.0)
        assertThat(c.annualYieldRate).isEqualTo(3.5)
        assertThat(c.currency).isEqualTo(Currency.KRW)
        assertThat(c.accountLocalId).isEqualTo(1L)
    }

    @Test
    fun `toSnapshot - unknown currency code falls back to USD`() {
        val snapshot = mapper.toSnapshot(
            displayName = "x", updatedAt = 0L, accounts = emptyList(),
            holdings = listOf(
                HoldingEntity(
                    id = 1L, accountId = 1L, symbol = "X", name = "X",
                    quantity = 1, averagePrice = 1.0, currency = "JPY"
                )
            ),
            cashItems = listOf(
                CashItemEntity(
                    id = 1L, accountId = 1L, name = "c",
                    originalValue = 1.0, annualYieldRate = 0.0, currency = "JPY"
                )
            )
        )

        assertThat(snapshot.holdings.single().currency).isEqualTo(Currency.USD)
        assertThat(snapshot.cashItems.single().currency).isEqualTo(Currency.USD)
    }
}
