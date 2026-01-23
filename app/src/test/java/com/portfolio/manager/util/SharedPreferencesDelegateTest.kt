package com.portfolio.manager.util

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SharedPreferencesDelegateTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        editor = mockk(relaxed = true) {
            every { putBoolean(any(), any()) } returns this
            every { putInt(any(), any()) } returns this
            every { putFloat(any(), any()) } returns this
        }
        sharedPreferences = mockk {
            every { edit() } returns editor
        }
    }

    // Boolean delegate tests
    @Test
    fun `boolean - getValue - returns stored value`() {
        every { sharedPreferences.getBoolean("test_key", false) } returns true

        val delegate = sharedPreferences.boolean("test_key", false)
        val value by delegate

        assertThat(value).isTrue()
    }

    @Test
    fun `boolean - getValue - returns default when not set`() {
        every { sharedPreferences.getBoolean("test_key", true) } returns true

        val delegate = sharedPreferences.boolean("test_key", true)
        val value by delegate

        assertThat(value).isTrue()
    }

    @Test
    fun `boolean - setValue - stores value`() {
        val delegate = sharedPreferences.boolean("test_key", false)
        var value by delegate
        value = true

        verify { editor.putBoolean("test_key", true) }
        verify { editor.apply() }
    }

    // Int delegate tests
    @Test
    fun `int - getValue - returns stored value`() {
        every { sharedPreferences.getInt("test_key", 0) } returns 42

        val delegate = sharedPreferences.int("test_key", 0)
        val value by delegate

        assertThat(value).isEqualTo(42)
    }

    @Test
    fun `int - getValue - returns default when not set`() {
        every { sharedPreferences.getInt("test_key", 10) } returns 10

        val delegate = sharedPreferences.int("test_key", 10)
        val value by delegate

        assertThat(value).isEqualTo(10)
    }

    @Test
    fun `int - setValue - stores value`() {
        val delegate = sharedPreferences.int("test_key", 0)
        var value by delegate
        value = 100

        verify { editor.putInt("test_key", 100) }
        verify { editor.apply() }
    }

    // Float delegate tests
    @Test
    fun `float - getValue - returns stored value`() {
        every { sharedPreferences.getFloat("test_key", 0f) } returns 3.14f

        val delegate = sharedPreferences.float("test_key", 0f)
        val value by delegate

        assertThat(value).isEqualTo(3.14f)
    }

    @Test
    fun `float - getValue - returns default when not set`() {
        every { sharedPreferences.getFloat("test_key", 1.5f) } returns 1.5f

        val delegate = sharedPreferences.float("test_key", 1.5f)
        val value by delegate

        assertThat(value).isEqualTo(1.5f)
    }

    @Test
    fun `float - setValue - stores value`() {
        val delegate = sharedPreferences.float("test_key", 0f)
        var value by delegate
        value = 2.71f

        verify { editor.putFloat("test_key", 2.71f) }
        verify { editor.apply() }
    }

    // Double delegate tests
    @Test
    fun `double - getValue - returns stored value`() {
        every { sharedPreferences.getFloat("test_key", 0f) } returns 3.14f

        val delegate = sharedPreferences.double("test_key", 0.0)
        val value by delegate

        assertThat(value).isEqualTo(3.14f.toDouble())
    }

    @Test
    fun `double - getValue - returns default when not set`() {
        every { sharedPreferences.getFloat("test_key", 7.0f) } returns 7.0f

        val delegate = sharedPreferences.double("test_key", 7.0)
        val value by delegate

        assertThat(value).isEqualTo(7.0)
    }

    @Test
    fun `double - setValue - stores value as float`() {
        val delegate = sharedPreferences.double("test_key", 0.0)
        var value by delegate
        value = 2.5

        verify { editor.putFloat("test_key", 2.5f) }
        verify { editor.apply() }
    }

    // Enum delegate tests
    private enum class TestEnum { FIRST, SECOND, THIRD }

    @Test
    fun `enum - getValue - returns stored value`() {
        every { sharedPreferences.getInt("test_key", 0) } returns 1

        val delegate = sharedPreferences.enum("test_key", TestEnum.FIRST)
        val value by delegate

        assertThat(value).isEqualTo(TestEnum.SECOND)
    }

    @Test
    fun `enum - getValue - returns default when not set`() {
        every { sharedPreferences.getInt("test_key", 2) } returns 2

        val delegate = sharedPreferences.enum("test_key", TestEnum.THIRD)
        val value by delegate

        assertThat(value).isEqualTo(TestEnum.THIRD)
    }

    @Test
    fun `enum - getValue - returns default for invalid ordinal`() {
        every { sharedPreferences.getInt("test_key", 0) } returns 99

        val delegate = sharedPreferences.enum("test_key", TestEnum.FIRST)
        val value by delegate

        assertThat(value).isEqualTo(TestEnum.FIRST)
    }

    @Test
    fun `enum - setValue - stores ordinal`() {
        val delegate = sharedPreferences.enum("test_key", TestEnum.FIRST)
        var value by delegate
        value = TestEnum.THIRD

        verify { editor.putInt("test_key", 2) }
        verify { editor.apply() }
    }
}
