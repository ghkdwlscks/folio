package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.CashItemEntity
import org.junit.Test

class CashItemMapperTest {

    @Test
    fun `fromEntity - converts entity to domain model`() {
        val entity = CashItemEntity(
            id = 1,
            accountId = 2,
            name = "Emergency Fund",
            originalValue = 10000.0,
            annualYieldRate = 4.5,
            currency = "USD",
            createdAt = 1000L
        )

        val cashItem = CashItemMapper.fromEntity(entity)

        assertThat(cashItem.id).isEqualTo(1)
        assertThat(cashItem.accountId).isEqualTo(2)
        assertThat(cashItem.name).isEqualTo("Emergency Fund")
        assertThat(cashItem.originalValue).isEqualTo(10000.0)
        assertThat(cashItem.annualYieldRate).isEqualTo(4.5)
        assertThat(cashItem.currency).isEqualTo("USD")
        assertThat(cashItem.createdAt).isEqualTo(1000L)
    }

    @Test
    fun `fromEntities - converts list of entities`() {
        val entities = listOf(
            CashItemEntity(
                id = 1,
                accountId = 1,
                name = "Savings",
                originalValue = 5000.0,
                annualYieldRate = 3.0,
                currency = "USD",
                createdAt = 1000L
            ),
            CashItemEntity(
                id = 2,
                accountId = 1,
                name = "Emergency",
                originalValue = 10000.0,
                annualYieldRate = 4.0,
                currency = "USD",
                createdAt = 2000L
            )
        )

        val cashItems = CashItemMapper.fromEntities(entities)

        assertThat(cashItems).hasSize(2)
        assertThat(cashItems[0].name).isEqualTo("Savings")
        assertThat(cashItems[1].name).isEqualTo("Emergency")
    }

    @Test
    fun `fromEntities - returns empty list for empty input`() {
        val cashItems = CashItemMapper.fromEntities(emptyList())

        assertThat(cashItems).isEmpty()
    }
}
