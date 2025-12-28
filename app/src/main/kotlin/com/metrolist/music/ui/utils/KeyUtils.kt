/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import java.util.concurrent.atomic.AtomicLong

/**
 * Utility object for generating unique keys in LazyColumn/LazyRow to prevent duplicate key errors
 */
object KeyUtils {
    private val counter = AtomicLong(0)
    
    /**
     * Generates a unique key by combining a base identifier with a unique counter
     * This prevents duplicate keys in LazyColumn/LazyRow implementations
     */
    fun generateUniqueKey(baseId: String, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_$uniqueId"
        } else {
            "${baseId}_$uniqueId"
        }
    }
    
    /**
     * Generates a unique key for items in a list with their index
     * Useful for preventing duplicate keys when items might have the same ID
     */
    fun generateIndexedKey(baseId: String, index: Int, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${index}_$uniqueId"
        } else {
            "${baseId}_${index}_$uniqueId"
        }
    }
    
    /**
     * Generates a timestamp-based unique key for dynamic content
     * Useful for content that changes frequently
     */
    fun generateTimestampKey(baseId: String, prefix: String = ""): String {
        val timestamp = System.currentTimeMillis()
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${timestamp}_$uniqueId"
        } else {
            "${baseId}_${timestamp}_$uniqueId"
        }
    }
}
