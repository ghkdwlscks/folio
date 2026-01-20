package com.portfolio.manager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppConstantsTest {

    @Test
    fun `ALL_ACCOUNTS_ID - has expected value`() {
        assertThat(AppConstants.ALL_ACCOUNTS_ID).isEqualTo(-1L)
    }
}
