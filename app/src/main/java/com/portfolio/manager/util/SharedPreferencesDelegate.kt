package com.portfolio.manager.util

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun SharedPreferences.boolean(
    key: String,
    defaultValue: Boolean = false
): ReadWriteProperty<Any?, Boolean> = object : ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        getBoolean(key, defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        edit().putBoolean(key, value).apply()
    }
}

fun SharedPreferences.int(
    key: String,
    defaultValue: Int = 0
): ReadWriteProperty<Any?, Int> = object : ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int =
        getInt(key, defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        edit().putInt(key, value).apply()
    }
}

fun SharedPreferences.float(
    key: String,
    defaultValue: Float = 0f
): ReadWriteProperty<Any?, Float> = object : ReadWriteProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float =
        getFloat(key, defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        edit().putFloat(key, value).apply()
    }
}

fun SharedPreferences.double(
    key: String,
    defaultValue: Double = 0.0
): ReadWriteProperty<Any?, Double> = object : ReadWriteProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double =
        getFloat(key, defaultValue.toFloat()).toDouble()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        edit().putFloat(key, value.toFloat()).apply()
    }
}

inline fun <reified T : Enum<T>> SharedPreferences.enum(
    key: String,
    defaultValue: T
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val ordinal = getInt(key, defaultValue.ordinal)
        return enumValues<T>().getOrElse(ordinal) { defaultValue }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        edit().putInt(key, value.ordinal).apply()
    }
}
