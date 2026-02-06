package com.portfolio.manager.domain.service

/**
 * Generic cache for storing data per account ID.
 * Provides a clean API for account-specific caching operations.
 */
class PerAccountCache<T> {
    private val cache = mutableMapOf<Long, T>()

    /**
     * Gets the cached value for the given account ID, or null if not present.
     */
    operator fun get(accountId: Long): T? = cache[accountId]

    /**
     * Sets the cached value for the given account ID.
     */
    operator fun set(accountId: Long, value: T) {
        cache[accountId] = value
    }

    /**
     * Updates the cached value for the given account ID using a transform function.
     * The transform receives the current value (or null) and should return the new value.
     * If the transform returns null, the entry is removed.
     */
    fun update(accountId: Long, transform: (T?) -> T?) {
        val current = cache[accountId]
        val updated = transform(current)
        if (updated != null) {
            cache[accountId] = updated
        } else {
            cache.remove(accountId)
        }
    }

    /**
     * Gets the cached value or computes and caches a default value if not present.
     */
    fun getOrPut(accountId: Long, defaultValue: () -> T): T {
        return cache.getOrPut(accountId, defaultValue)
    }

    /**
     * Removes the cached value for the given account ID.
     */
    fun remove(accountId: Long) {
        cache.remove(accountId)
    }

    /**
     * Removes all cached values.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Checks if there is a cached value for the given account ID.
     */
    fun contains(accountId: Long): Boolean = cache.containsKey(accountId)

    /**
     * Returns the number of cached entries.
     */
    val size: Int get() = cache.size

    /**
     * Checks if the cache is empty.
     */
    fun isEmpty(): Boolean = cache.isEmpty()

    /**
     * Returns all account IDs in the cache.
     */
    fun keys(): Set<Long> = cache.keys.toSet()
}
