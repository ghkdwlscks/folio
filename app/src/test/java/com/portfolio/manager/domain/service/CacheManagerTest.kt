package com.portfolio.manager.domain.service

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CacheManagerTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var cacheManager: CacheManager

    @Before
    fun setup() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        cacheManager = CacheManager(sharedPreferences)
    }

    @Test
    fun `save - stores JSON string`() {
        val data = listOf("item1", "item2")

        val result = cacheManager.save("test_key", data)

        assertThat(result).isTrue()
        verify { editor.putString("test_key", any()) }
        verify { editor.apply() }
    }

    @Test
    fun `save - returns false on serialization error`() {
        // Create an object that cannot be serialized (no @Serializable annotation)
        // Using a lambda which cannot be serialized
        val nonSerializable = object {
            val callback: () -> Unit = {}
        }

        val result = cacheManager.save("test_key", nonSerializable)

        assertThat(result).isFalse()
    }

    @Test
    fun `load - returns null when key not found`() {
        every { sharedPreferences.getString("missing_key", null) } returns null

        val result = cacheManager.load<List<String>>("missing_key")

        assertThat(result).isNull()
    }

    @Test
    fun `load - returns deserialized value`() {
        every { sharedPreferences.getString("test_key", null) } returns """["item1","item2"]"""

        val result = cacheManager.load<List<String>>("test_key")

        assertThat(result).containsExactly("item1", "item2")
    }

    @Test
    fun `load - returns null on deserialization error`() {
        every { sharedPreferences.getString("test_key", null) } returns "invalid json"

        val result = cacheManager.load<List<String>>("test_key")

        assertThat(result).isNull()
    }

    @Test
    fun `loadOrDefault - returns cached value when exists`() {
        every { sharedPreferences.getString("test_key", null) } returns """["item1"]"""

        val result = cacheManager.loadOrDefault("test_key", emptyList<String>())

        assertThat(result).containsExactly("item1")
    }

    @Test
    fun `loadOrDefault - returns default when key not found`() {
        every { sharedPreferences.getString("missing_key", null) } returns null

        val result = cacheManager.loadOrDefault("missing_key", listOf("default"))

        assertThat(result).containsExactly("default")
    }

    @Test
    fun `loadOrDefault - returns default on deserialization error`() {
        every { sharedPreferences.getString("test_key", null) } returns "invalid"

        val result = cacheManager.loadOrDefault("test_key", listOf("default"))

        assertThat(result).containsExactly("default")
    }

    @Test
    fun `saveFloat - stores float value`() {
        cacheManager.saveFloat("float_key", 3.14f)

        verify { editor.putFloat("float_key", 3.14f) }
        verify { editor.apply() }
    }

    @Test
    fun `loadFloat - returns cached float`() {
        every { sharedPreferences.getFloat("float_key", 0f) } returns 3.14f

        val result = cacheManager.loadFloat("float_key", 0f)

        assertThat(result).isEqualTo(3.14f)
    }

    @Test
    fun `loadFloat - returns default when not found`() {
        every { sharedPreferences.getFloat("missing_key", 1.0f) } returns 1.0f

        val result = cacheManager.loadFloat("missing_key", 1.0f)

        assertThat(result).isEqualTo(1.0f)
    }

    @Test
    fun `saveMultiple - saves all values atomically`() {
        cacheManager.saveMultiple {
            put("key1", listOf("a", "b"))
            put("key2", "value")
            putFloat("key3", 1.5f)
        }

        verify { editor.putString("key1", any()) }
        verify { editor.putString("key2", any()) }
        verify { editor.putFloat("key3", 1.5f) }
        verify(exactly = 1) { editor.apply() }
    }

    @Test
    fun `saveMultiple - continues on serialization error`() {
        cacheManager.saveMultiple {
            put("key1", listOf("valid"))
            putFloat("key2", 2.0f)
        }

        verify { editor.putString("key1", any()) }
        verify { editor.putFloat("key2", 2.0f) }
        verify { editor.apply() }
    }
}
