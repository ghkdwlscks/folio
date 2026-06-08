package com.portfolio.manager.domain.service

import javax.inject.Inject

import kotlin.random.Random

/**
 * Generates and validates household pairing codes (format: XXXX-XXXX) using an
 * unambiguous charset (no 0/O/1/I/L) so they are easy to read and type.
 */
class HouseholdCodeGenerator @Inject constructor() {

    companion object {
        private const val CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        private const val GROUP = 4
        private val FORMAT = Regex("[$CHARSET]{$GROUP}-[$CHARSET]{$GROUP}")
    }

    fun generate(random: Random = Random.Default): String {
        val chars = (1..(GROUP * 2)).map { CHARSET[random.nextInt(CHARSET.length)] }
        return chars.take(GROUP).joinToString("") + "-" + chars.drop(GROUP).joinToString("")
    }

    fun normalize(raw: String): String {
        val cleaned = raw.trim().uppercase().replace("-", "").replace(" ", "")
        return if (cleaned.length == GROUP * 2) {
            "${cleaned.substring(0, GROUP)}-${cleaned.substring(GROUP)}"
        } else {
            cleaned
        }
    }

    fun isValid(code: String): Boolean = FORMAT.matches(code)
}
