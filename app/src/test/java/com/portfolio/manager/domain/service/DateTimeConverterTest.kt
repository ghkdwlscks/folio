package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DateTimeConverterTest {

    @Test
    fun `timestampToDate - converts Unix timestamp to date string`() {
        val timestamp = 1705276800L // 2024-01-15 00:00:00 UTC
        val result = DateTimeConverter.timestampToDate(timestamp)
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}")
    }

    @Test
    fun `dateToTimestamp - converts valid date string to timestamp`() {
        val dateString = "2024-01-15"
        val result = DateTimeConverter.dateToTimestamp(dateString)

        assertThat(result).isNotNull()
        assertThat(result!!).isGreaterThan(1704067200L) // 2024-01-01
        assertThat(result).isLessThan(1735689600L) // 2025-01-01
    }

    @Test
    fun `dateToTimestamp - returns null for idx keys`() {
        val result = DateTimeConverter.dateToTimestamp("idx_5")
        assertThat(result).isNull()
    }

    @Test
    fun `dateToTimestamp - returns null for invalid date string`() {
        val result = DateTimeConverter.dateToTimestamp("invalid-date")
        assertThat(result).isNull()
    }

    @Test
    fun `dateToTimestamp - roundtrip with timestampToDate`() {
        val originalTimestamp = 1705276800L
        val dateString = DateTimeConverter.timestampToDate(originalTimestamp)
        val resultTimestamp = DateTimeConverter.dateToTimestamp(dateString)

        assertThat(resultTimestamp).isNotNull()
        assertThat(kotlin.math.abs(resultTimestamp!! - originalTimestamp)).isLessThan(86400L)
    }

    @Test
    fun `zipWithDateKeys - with timestamps - creates date-keyed map`() {
        val timestamps = listOf(1705276800L, 1705363200L)
        val values = listOf(100.0, 110.0)

        val result = DateTimeConverter.zipWithDateKeys(timestamps, values)

        assertThat(result).hasSize(2)
        assertThat(result.keys.all { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }).isTrue()
        assertThat(result.values).containsExactly(100.0, 110.0)
    }

    @Test
    fun `zipWithDateKeys - empty timestamps - uses index keys`() {
        val timestamps = emptyList<Long>()
        val values = listOf(100.0, 110.0)

        val result = DateTimeConverter.zipWithDateKeys(timestamps, values)

        assertThat(result).containsExactly("idx_0", 100.0, "idx_1", 110.0)
    }

    @Test
    fun `zipWithDateKeys - mismatched sizes - uses index keys`() {
        val timestamps = listOf(1705276800L)
        val values = listOf(100.0, 110.0)

        val result = DateTimeConverter.zipWithDateKeys(timestamps, values)

        assertThat(result).containsExactly("idx_0", 100.0, "idx_1", 110.0)
    }

    @Test
    fun `zipWithDateKeys - empty values - returns empty map`() {
        val timestamps = emptyList<Long>()
        val values = emptyList<Double>()

        val result = DateTimeConverter.zipWithDateKeys(timestamps, values)

        assertThat(result).isEmpty()
    }

    @Test
    fun `zipWithDateKeys - works with non-Double types`() {
        val timestamps = listOf(1705276800L, 1705363200L)
        val values = listOf("A", "B")

        val result = DateTimeConverter.zipWithDateKeys(timestamps, values)

        assertThat(result).hasSize(2)
        assertThat(result.values).containsExactly("A", "B")
    }
}
