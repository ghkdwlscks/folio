package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PerAccountCacheTest {

    @Test
    fun `get - returns null for missing key`() {
        val cache = PerAccountCache<String>()
        assertThat(cache[1L]).isNull()
    }

    @Test
    fun `set and get - stores and retrieves value`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value1"
        assertThat(cache[1L]).isEqualTo("value1")
    }

    @Test
    fun `set - overwrites existing value`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value1"
        cache[1L] = "value2"
        assertThat(cache[1L]).isEqualTo("value2")
    }

    @Test
    fun `update - transforms existing value`() {
        val cache = PerAccountCache<Int>()
        cache[1L] = 10
        cache.update(1L) { (it ?: 0) + 5 }
        assertThat(cache[1L]).isEqualTo(15)
    }

    @Test
    fun `update - creates value from null`() {
        val cache = PerAccountCache<Int>()
        cache.update(1L) { 42 }
        assertThat(cache[1L]).isEqualTo(42)
    }

    @Test
    fun `update - removes entry when transform returns null`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value"
        cache.update(1L) { null }
        assertThat(cache[1L]).isNull()
        assertThat(cache.contains(1L)).isFalse()
    }

    @Test
    fun `getOrPut - returns existing value`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "existing"
        val result = cache.getOrPut(1L) { "new" }
        assertThat(result).isEqualTo("existing")
    }

    @Test
    fun `getOrPut - computes and stores default for missing key`() {
        val cache = PerAccountCache<String>()
        val result = cache.getOrPut(1L) { "computed" }
        assertThat(result).isEqualTo("computed")
        assertThat(cache[1L]).isEqualTo("computed")
    }

    @Test
    fun `remove - removes existing entry`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value"
        cache.remove(1L)
        assertThat(cache[1L]).isNull()
    }

    @Test
    fun `remove - handles missing key gracefully`() {
        val cache = PerAccountCache<String>()
        cache.remove(1L)
        assertThat(cache[1L]).isNull()
    }

    @Test
    fun `clear - removes all entries`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value1"
        cache[2L] = "value2"
        cache.clear()
        assertThat(cache.isEmpty()).isTrue()
        assertThat(cache[1L]).isNull()
        assertThat(cache[2L]).isNull()
    }

    @Test
    fun `contains - returns true for existing key`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value"
        assertThat(cache.contains(1L)).isTrue()
    }

    @Test
    fun `contains - returns false for missing key`() {
        val cache = PerAccountCache<String>()
        assertThat(cache.contains(1L)).isFalse()
    }

    @Test
    fun `size - returns correct count`() {
        val cache = PerAccountCache<String>()
        assertThat(cache.size).isEqualTo(0)
        cache[1L] = "value1"
        assertThat(cache.size).isEqualTo(1)
        cache[2L] = "value2"
        assertThat(cache.size).isEqualTo(2)
    }

    @Test
    fun `isEmpty - returns true for empty cache`() {
        val cache = PerAccountCache<String>()
        assertThat(cache.isEmpty()).isTrue()
    }

    @Test
    fun `isEmpty - returns false for non-empty cache`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value"
        assertThat(cache.isEmpty()).isFalse()
    }

    @Test
    fun `keys - returns all account IDs`() {
        val cache = PerAccountCache<String>()
        cache[1L] = "value1"
        cache[2L] = "value2"
        cache[3L] = "value3"
        assertThat(cache.keys()).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `keys - returns empty set for empty cache`() {
        val cache = PerAccountCache<String>()
        assertThat(cache.keys()).isEmpty()
    }

    @Test
    fun `works with complex types`() {
        val cache = PerAccountCache<Map<String, Double>>()
        cache[1L] = mapOf("AAPL" to 10.0, "GOOG" to 20.0)
        cache.update(1L) { current ->
            current?.toMutableMap()?.apply { put("MSFT", 30.0) }
        }
        assertThat(cache[1L]).containsExactly("AAPL", 10.0, "GOOG", 20.0, "MSFT", 30.0)
    }
}
