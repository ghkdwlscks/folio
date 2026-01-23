package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SortOptionTest {

    @Test
    fun `entries - contains all sort options`() {
        assertThat(SortOption.entries).hasSize(5)
    }

    @Test
    fun `WEIGHT - has correct label`() {
        assertThat(SortOption.WEIGHT.label).isEqualTo("Weight")
    }

    @Test
    fun `NAME - has correct label`() {
        assertThat(SortOption.NAME.label).isEqualTo("Name")
    }

    @Test
    fun `SYMBOL - has correct label`() {
        assertThat(SortOption.SYMBOL.label).isEqualTo("Symbol")
    }

    @Test
    fun `GAIN_LOSS_PERCENT - has correct label`() {
        assertThat(SortOption.GAIN_LOSS_PERCENT.label).isEqualTo("Gain/Loss %")
    }

    @Test
    fun `DAY_CHANGE_PERCENT - has correct label`() {
        assertThat(SortOption.DAY_CHANGE_PERCENT.label).isEqualTo("Day Change %")
    }

    @Test
    fun `ordinal values - are sequential`() {
        assertThat(SortOption.WEIGHT.ordinal).isEqualTo(0)
        assertThat(SortOption.NAME.ordinal).isEqualTo(1)
        assertThat(SortOption.SYMBOL.ordinal).isEqualTo(2)
        assertThat(SortOption.GAIN_LOSS_PERCENT.ordinal).isEqualTo(3)
        assertThat(SortOption.DAY_CHANGE_PERCENT.ordinal).isEqualTo(4)
    }
}
