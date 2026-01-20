package com.portfolio.manager.util

fun String.isKoreanStock(): Boolean = endsWith(".KS") || endsWith(".KQ")
