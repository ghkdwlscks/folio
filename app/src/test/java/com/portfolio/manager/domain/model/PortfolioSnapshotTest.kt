package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.util.JsonSerializer
import kotlinx.serialization.encodeToString
import org.junit.Test

class PortfolioSnapshotTest {

    private val json = JsonSerializer.instance

    @Test
    fun `snapshot - round trips through json`() {
        val snapshot = PortfolioSnapshot(
            displayName = "남편",
            updatedAt = 123L,
            schemaVersion = 1,
            accounts = listOf(SnapshotAccount(localId = 1L, name = "Retirement", order = 0)),
            holdings = listOf(
                SnapshotHolding(
                    localId = 10L,
                    accountLocalId = 1L,
                    symbol = "AAPL",
                    name = "Apple Inc.",
                    quantity = 5,
                    averagePrice = 150.0,
                    currency = Currency.USD,
                    targetPercentage = 20
                )
            ),
            cashItems = listOf(
                SnapshotCashItem(
                    localId = 100L,
                    accountLocalId = 1L,
                    name = "Savings",
                    value = 1000.0,
                    annualYieldRate = 3.5,
                    currency = Currency.KRW
                )
            )
        )

        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString(PortfolioSnapshot.serializer(), encoded)

        assertThat(decoded).isEqualTo(snapshot)
    }

    @Test
    fun `snapshot - defaults are empty`() {
        val snapshot = PortfolioSnapshot(displayName = "와이프")

        assertThat(snapshot.updatedAt).isEqualTo(0L)
        assertThat(snapshot.schemaVersion).isEqualTo(1)
        assertThat(snapshot.accounts).isEmpty()
        assertThat(snapshot.holdings).isEmpty()
        assertThat(snapshot.cashItems).isEmpty()
    }
}
