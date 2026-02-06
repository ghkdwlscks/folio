package com.portfolio.manager.domain.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Service for date and timestamp conversion operations.
 */
object DateTimeConverter {

    /**
     * Converts Unix timestamp (seconds) to date string YYYY-MM-DD.
     */
    fun timestampToDate(timestamp: Long): String {
        val instant = Instant.ofEpochSecond(timestamp)
        return LocalDate.ofInstant(instant, ZoneId.of("UTC")).toString()
    }

    /**
     * Converts date string YYYY-MM-DD to Unix timestamp (seconds).
     * Returns null for invalid date strings or index-based keys.
     */
    fun dateToTimestamp(dateString: String): Long? {
        if (dateString.startsWith("idx_")) return null
        return try {
            val localDate = LocalDate.parse(dateString)
            localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().epochSecond
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a list of timestamps and values to a date-keyed map.
     * Falls back to index-based keys if timestamps are empty or mismatched.
     */
    fun <T> zipWithDateKeys(timestamps: List<Long>, values: List<T>): Map<String, T> {
        return if (timestamps.isNotEmpty() && timestamps.size == values.size) {
            timestamps.zip(values).associate { (ts, value) ->
                timestampToDate(ts) to value
            }
        } else {
            values.mapIndexed { index, value ->
                "idx_$index" to value
            }.toMap()
        }
    }
}
