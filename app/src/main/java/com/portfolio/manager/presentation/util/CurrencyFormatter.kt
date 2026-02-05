package com.portfolio.manager.presentation.util

import java.text.NumberFormat
import java.util.Locale

import com.portfolio.manager.domain.model.Currency

object CurrencyFormatter {

    fun format(amount: Double, currency: Currency): String {
        return if (currency.isKrw) {
            val format = NumberFormat.getNumberInstance(Locale.KOREA).apply {
                maximumFractionDigits = 0
            }
            "₩${format.format(amount)}"
        } else {
            NumberFormat.getCurrencyInstance(Locale.US).format(amount)
        }
    }

    fun formatUsd(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }

    fun formatKrw(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.KOREA).apply {
            maximumFractionDigits = 0
        }
        return "₩${format.format(amount)}"
    }

    fun formatPercent(value: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(value)
    }

    fun createFormatter(showInKrw: Boolean): (Double) -> String =
        if (showInKrw) ::formatKrw else ::formatUsd
}
