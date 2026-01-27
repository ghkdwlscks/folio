package com.portfolio.manager.domain.service

import android.content.SharedPreferences
import android.util.Log
import com.portfolio.manager.util.JsonSerializer
import kotlinx.serialization.encodeToString

/**
 * Service for managing JSON-based caching with SharedPreferences.
 */
class CacheManager(
    @PublishedApi internal val sharedPreferences: SharedPreferences
) {
    companion object {
        @PublishedApi internal const val TAG = "CacheManager"
    }

    @PublishedApi internal val json = JsonSerializer.instance

    /**
     * Saves a serializable value to cache.
     * @param key The preference key
     * @param value The value to cache
     * @return true if successful, false otherwise
     */
    inline fun <reified T> save(key: String, value: T): Boolean {
        return try {
            val jsonString = json.encodeToString(value)
            sharedPreferences.edit()
                .putString(key, jsonString)
                .apply()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save cache for key: $key", e)
            false
        }
    }

    /**
     * Loads a cached value.
     * @param key The preference key
     * @return The cached value or null if not found or failed to deserialize
     */
    inline fun <reified T> load(key: String): T? {
        return try {
            val jsonString = sharedPreferences.getString(key, null) ?: return null
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cache for key: $key", e)
            null
        }
    }

    /**
     * Loads a cached value with a default fallback.
     * @param key The preference key
     * @param default The default value if not found or failed
     * @return The cached value or the default
     */
    inline fun <reified T> loadOrDefault(key: String, default: T): T {
        return load(key) ?: default
    }

    /**
     * Saves a float value to cache.
     * @param key The preference key
     * @param value The value to cache
     */
    fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit()
            .putFloat(key, value)
            .apply()
    }

    /**
     * Loads a cached float value.
     * @param key The preference key
     * @param default The default value if not found
     * @return The cached value or the default
     */
    fun loadFloat(key: String, default: Float): Float {
        return sharedPreferences.getFloat(key, default)
    }

    /**
     * Saves multiple values atomically.
     * @param block A function that performs multiple save operations
     */
    fun saveMultiple(block: CacheTransaction.() -> Unit) {
        val transaction = CacheTransaction(sharedPreferences.edit(), json)
        block(transaction)
        transaction.apply()
    }

    class CacheTransaction(
        @PublishedApi internal val editor: SharedPreferences.Editor,
        @PublishedApi internal val json: kotlinx.serialization.json.Json
    ) {
        inline fun <reified T> put(key: String, value: T) {
            try {
                val jsonString = json.encodeToString(value)
                editor.putString(key, jsonString)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to serialize value for key: $key", e)
            }
        }

        fun putFloat(key: String, value: Float) {
            editor.putFloat(key, value)
        }

        fun apply() {
            editor.apply()
        }
    }
}
