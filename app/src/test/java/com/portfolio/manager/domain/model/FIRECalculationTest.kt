package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FIRECalculationTest {

    @Test
    fun `FIRECalculation - stores all values correctly`() {
        val calculation = FIRECalculation(
            totalPortfolioValue = 100000.0,
            annualReturn = 7.0,
            annualInflation = 3.0,
            realReturn = 4.0,
            sustainableMonthlySpending = 333.33,
            sustainableAnnualSpending = 4000.0
        )

        assertThat(calculation.totalPortfolioValue).isEqualTo(100000.0)
        assertThat(calculation.annualReturn).isEqualTo(7.0)
        assertThat(calculation.annualInflation).isEqualTo(3.0)
        assertThat(calculation.realReturn).isEqualTo(4.0)
        assertThat(calculation.sustainableMonthlySpending).isEqualTo(333.33)
        assertThat(calculation.sustainableAnnualSpending).isEqualTo(4000.0)
    }

    @Test
    fun `FIRETargetCalculation - stores all values correctly`() {
        val calculation = FIRETargetCalculation(
            targetMonthlySpending = 5000.0,
            requiredPortfolio = 1500000.0,
            currentPortfolio = 300000.0,
            progressPercent = 20.0,
            remainingAmount = 1200000.0
        )

        assertThat(calculation.targetMonthlySpending).isEqualTo(5000.0)
        assertThat(calculation.requiredPortfolio).isEqualTo(1500000.0)
        assertThat(calculation.currentPortfolio).isEqualTo(300000.0)
        assertThat(calculation.progressPercent).isEqualTo(20.0)
        assertThat(calculation.remainingAmount).isEqualTo(1200000.0)
    }

    @Test
    fun `FIRECalculation - equality check`() {
        val calc1 = FIRECalculation(100000.0, 7.0, 3.0, 4.0, 333.33, 4000.0)
        val calc2 = FIRECalculation(100000.0, 7.0, 3.0, 4.0, 333.33, 4000.0)
        val calc3 = FIRECalculation(200000.0, 7.0, 3.0, 4.0, 333.33, 4000.0)

        assertThat(calc1).isEqualTo(calc2)
        assertThat(calc1).isNotEqualTo(calc3)
    }

    @Test
    fun `FIRETargetCalculation - equality check`() {
        val calc1 = FIRETargetCalculation(5000.0, 1500000.0, 300000.0, 20.0, 1200000.0)
        val calc2 = FIRETargetCalculation(5000.0, 1500000.0, 300000.0, 20.0, 1200000.0)
        val calc3 = FIRETargetCalculation(6000.0, 1500000.0, 300000.0, 20.0, 1200000.0)

        assertThat(calc1).isEqualTo(calc2)
        assertThat(calc1).isNotEqualTo(calc3)
    }
}
