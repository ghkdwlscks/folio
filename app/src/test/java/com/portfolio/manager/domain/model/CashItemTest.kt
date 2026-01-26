package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CashItemTest {

    @Test
    fun `valueInUsd - USD currency - returns value directly`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 0.0,
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        assertThat(cashItem.valueInUsd(1400.0)).isWithin(0.01).of(10000.0)
    }

    @Test
    fun `valueInUsd - KRW currency - converts to USD`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 14000000.0, // 14 million KRW
            annualYieldRate = 0.0,
            currency = "KRW",
            createdAt = System.currentTimeMillis()
        )

        // 14,000,000 KRW / 1400 = 10,000 USD
        assertThat(cashItem.valueInUsd(1400.0)).isWithin(0.01).of(10000.0)
    }

    @Test
    fun `valueInKrw - KRW currency - returns value directly`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 14000000.0,
            annualYieldRate = 0.0,
            currency = "KRW",
            createdAt = System.currentTimeMillis()
        )

        assertThat(cashItem.valueInKrw(1400.0)).isWithin(0.01).of(14000000.0)
    }

    @Test
    fun `valueInKrw - USD currency - converts to KRW`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 0.0,
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        // 10,000 USD * 1400 = 14,000,000 KRW
        assertThat(cashItem.valueInKrw(1400.0)).isWithin(0.01).of(14000000.0)
    }

    @Test
    fun `periodReturnPercent - ONE_WEEK - returns weekly rate`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 5.2, // 5.2% annual
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        // Weekly rate = 5.2 / 52 = 0.1
        assertThat(cashItem.periodReturnPercent(TimePeriod.ONE_WEEK)).isWithin(0.001).of(0.1)
    }

    @Test
    fun `periodReturnPercent - ONE_MONTH - returns monthly rate`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 12.0, // 12% annual
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        // Monthly rate = 12 / 12 = 1
        assertThat(cashItem.periodReturnPercent(TimePeriod.ONE_MONTH)).isWithin(0.001).of(1.0)
    }

    @Test
    fun `periodReturnPercent - THREE_MONTHS - returns quarterly rate`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 8.0, // 8% annual
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        // Quarterly rate = 8 / 4 = 2
        assertThat(cashItem.periodReturnPercent(TimePeriod.THREE_MONTHS)).isWithin(0.001).of(2.0)
    }

    @Test
    fun `periodReturnPercent - SIX_MONTHS - returns semi-annual rate`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 10.0, // 10% annual
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        // Semi-annual rate = 10 / 2 = 5
        assertThat(cashItem.periodReturnPercent(TimePeriod.SIX_MONTHS)).isWithin(0.001).of(5.0)
    }

    @Test
    fun `periodReturnPercent - ONE_YEAR - returns annual rate`() {
        val cashItem = CashItem(
            id = 1,
            accountId = 1,
            name = "Test",
            originalValue = 10000.0,
            annualYieldRate = 4.5, // 4.5% annual
            currency = "USD",
            createdAt = System.currentTimeMillis()
        )

        assertThat(cashItem.periodReturnPercent(TimePeriod.ONE_YEAR)).isWithin(0.001).of(4.5)
    }

    @Test
    fun `fromEntity - converts entity to domain model`() {
        val entity = com.portfolio.manager.data.local.CashItemEntity(
            id = 1,
            accountId = 2,
            name = "Emergency Fund",
            originalValue = 10000.0,
            annualYieldRate = 4.5,
            currency = "USD",
            createdAt = 1000L
        )

        val cashItem = CashItem.fromEntity(entity)

        assertThat(cashItem.id).isEqualTo(1)
        assertThat(cashItem.accountId).isEqualTo(2)
        assertThat(cashItem.name).isEqualTo("Emergency Fund")
        assertThat(cashItem.originalValue).isEqualTo(10000.0)
        assertThat(cashItem.annualYieldRate).isEqualTo(4.5)
        assertThat(cashItem.currency).isEqualTo("USD")
        assertThat(cashItem.createdAt).isEqualTo(1000L)
    }
}
