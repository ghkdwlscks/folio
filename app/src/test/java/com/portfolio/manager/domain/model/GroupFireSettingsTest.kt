package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.util.JsonSerializer
import kotlinx.serialization.encodeToString
import org.junit.Test

class GroupFireSettingsTest {

    private val json = JsonSerializer.instance

    @Test
    fun `group fire settings - round trips through json`() {
        val settings = GroupFireSettings(
            annualReturn = 7.5,
            annualInflation = 2.5,
            targetMonthlySpending = 3500.0,
            targetSpendingInKrw = true
        )

        val encoded = json.encodeToString(settings)
        val decoded = json.decodeFromString<GroupFireSettings>(encoded)

        assertThat(decoded).isEqualTo(settings)
    }

    @Test
    fun `group fire settings - exposes its properties`() {
        val settings = GroupFireSettings(
            annualReturn = 6.0,
            annualInflation = 1.5,
            targetMonthlySpending = 2000.0,
            targetSpendingInKrw = false
        )

        assertThat(settings.annualReturn).isEqualTo(6.0)
        assertThat(settings.annualInflation).isEqualTo(1.5)
        assertThat(settings.targetMonthlySpending).isEqualTo(2000.0)
        assertThat(settings.targetSpendingInKrw).isFalse()
    }
}
