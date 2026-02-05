package com.portfolio.manager.domain.model

import kotlinx.serialization.Serializable

/**
 * Type-safe representation of supported currencies.
 */
@Serializable
enum class Currency(val code: String, val symbol: String) {
    USD("USD", "$"),
    KRW("KRW", "₩");

    companion object {
        /**
         * Parses a currency code string to Currency enum.
         * @throws IllegalArgumentException if code is not recognized
         */
        fun fromCode(code: String): Currency =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown currency code: $code")

        /**
         * Safely parses a currency code, returning null if not recognized.
         */
        fun fromCodeOrNull(code: String): Currency? =
            entries.firstOrNull { it.code == code }
    }

    val isKrw: Boolean get() = this == KRW
    val isUsd: Boolean get() = this == USD
}
