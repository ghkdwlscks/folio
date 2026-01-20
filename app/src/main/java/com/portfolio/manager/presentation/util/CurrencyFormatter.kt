package com.portfolio.manager.presentation.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, currency: String): String {
        return when (currency) {
            "KRW" -> {
                val format = NumberFormat.getNumberInstance(Locale.KOREA).apply {
                    maximumFractionDigits = 0
                }
                "₩${format.format(amount)}"
            }
            else -> {
                NumberFormat.getCurrencyInstance(Locale.US).format(amount)
            }
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
}
