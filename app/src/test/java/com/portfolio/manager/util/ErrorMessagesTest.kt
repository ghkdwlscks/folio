package com.portfolio.manager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorMessagesTest {

    @Test
    fun `invalidQuantity - formats message correctly`() {
        val message = ErrorMessages.invalidQuantity(1, 1000000)
        assertThat(message).isEqualTo("Quantity must be between 1 and 1000000")
    }

    @Test
    fun `invalidPrice - formats message with decimals`() {
        val message = ErrorMessages.invalidPrice(0.0001, 1000000000.0)
        assertThat(message).isEqualTo("Price must be between 0.0001 and 1000000000")
    }

    @Test
    fun `constants - have expected values`() {
        assertThat(ErrorMessages.EMPTY_NAME).isEqualTo("Please enter a name")
        assertThat(ErrorMessages.INVALID_VALUE).isEqualTo("Please enter a valid value")
        assertThat(ErrorMessages.INVALID_YIELD).isEqualTo("Please enter a valid yield rate")
        assertThat(ErrorMessages.SELECT_ACCOUNT).isEqualTo("Please select an account")
        assertThat(ErrorMessages.DUPLICATE_SYMBOL).isEqualTo("This stock already exists in the account")
        assertThat(ErrorMessages.LOAD_PRICES_FAILED).isEqualTo("Failed to load prices")
        assertThat(ErrorMessages.NO_PORTFOLIO_DATA).isEqualTo("No portfolio data. Please refresh the dashboard first.")
    }
}
