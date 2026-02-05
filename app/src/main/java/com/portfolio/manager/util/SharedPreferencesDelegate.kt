package com.portfolio.manager.util

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Creates a generic SharedPreferences property delegate.
 * Used internally to reduce boilerplate in type-specific delegates.
 */
@PublishedApi
internal fun <T> sharedPref(
    getter: () -> T,
    setter: (T) -> Unit
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setter(value)
}

fun SharedPreferences.boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any?, Boolean> =
    sharedPref(
        getter = { getBoolean(key, defaultValue) },
        setter = { edit().putBoolean(key, it).apply() }
    )

fun SharedPreferences.int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any?, Int> =
    sharedPref(
        getter = { getInt(key, defaultValue) },
        setter = { edit().putInt(key, it).apply() }
    )

fun SharedPreferences.float(key: String, defaultValue: Float = 0f): ReadWriteProperty<Any?, Float> =
    sharedPref(
        getter = { getFloat(key, defaultValue) },
        setter = { edit().putFloat(key, it).apply() }
    )

fun SharedPreferences.double(key: String, defaultValue: Double = 0.0): ReadWriteProperty<Any?, Double> =
    sharedPref(
        getter = {
            // Try String first (new format), fall back to Float (old format) for migration
            try {
                getString(key, null)?.toDoubleOrNull() ?: defaultValue
            } catch (e: ClassCastException) {
                // Migrate from old Float storage to new String storage
                val floatValue = getFloat(key, defaultValue.toFloat()).toDouble()
                edit().remove(key).putString(key, floatValue.toString()).apply()
                floatValue
            }
        },
        setter = { edit().putString(key, it.toString()).apply() }
    )

inline fun <reified T : Enum<T>> SharedPreferences.enum(key: String, defaultValue: T): ReadWriteProperty<Any?, T> =
    sharedPref(
        getter = {
            val ordinal = getInt(key, defaultValue.ordinal)
            enumValues<T>().getOrElse(ordinal) { defaultValue }
        },
        setter = { edit().putInt(key, it.ordinal).apply() }
    )
