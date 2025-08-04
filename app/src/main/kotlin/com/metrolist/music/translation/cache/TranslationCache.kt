package com.metrolist.music.translation.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Translation Cache Manager
 * Handles caching of translations to avoid repeated API calls
 */
object TranslationCache {
    
    // In-memory cache for fast access
    private val memoryCache = ConcurrentHashMap<String, CachedTranslation>()
    
    // Maximum memory cache size to prevent memory overflow
    private const val MAX_MEMORY_CACHE_SIZE = 1000
    
    /**
     * Data class for cached translation
     */
    data class CachedTranslation(
        val originalText: String,
        val translatedText: String,
        val sourceLang: String,
        val targetLang: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Generate cache key from text and language pair
     */
    private fun generateCacheKey(text: String, sourceLang: String, targetLang: String): String {
        val input = "${text.trim().lowercase()}|$sourceLang|$targetLang"
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get translation from cache
     */
    suspend fun getTranslation(text: String, sourceLang: String, targetLang: String): String? = withContext(Dispatchers.IO) {
        val key = generateCacheKey(text, sourceLang, targetLang)
        return@withContext memoryCache[key]?.translatedText
    }
    
    /**
     * Store translation in cache
     */
    suspend fun storeTranslation(
        originalText: String, 
        translatedText: String, 
        sourceLang: String, 
        targetLang: String
    ) = withContext(Dispatchers.IO) {
        val key = generateCacheKey(originalText, sourceLang, targetLang)
        val cachedTranslation = CachedTranslation(
            originalText = originalText,
            translatedText = translatedText,
            sourceLang = sourceLang,
            targetLang = targetLang
        )
        
        // Add to memory cache
        memoryCache[key] = cachedTranslation
        
        // Clean up memory cache if it gets too large
        if (memoryCache.size > MAX_MEMORY_CACHE_SIZE) {
            cleanupMemoryCache()
        }
    }
    
    /**
     * Check if translation exists in cache
     */
    suspend fun hasTranslation(text: String, sourceLang: String, targetLang: String): Boolean = withContext(Dispatchers.IO) {
        val key = generateCacheKey(text, sourceLang, targetLang)
        return@withContext memoryCache.containsKey(key)
    }
    
    /**
     * Clear all translations for specific language pair
     */
    suspend fun clearTranslationsForLanguage(sourceLang: String, targetLang: String) = withContext(Dispatchers.IO) {
        val keysToRemove = memoryCache.entries
            .filter { it.value.sourceLang == sourceLang && it.value.targetLang == targetLang }
            .map { it.key }
        
        keysToRemove.forEach { key ->
            memoryCache.remove(key)
        }
    }
    
    /**
     * Clear all cached translations
     */
    suspend fun clearAllTranslations() = withContext(Dispatchers.IO) {
        memoryCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalEntries = memoryCache.size,
            memoryUsageKB = (memoryCache.size * 100) / 1024 // Rough estimate
        )
    }
    
    /**
     * Clean up old entries from memory cache
     */
    private fun cleanupMemoryCache() {
        if (memoryCache.size <= MAX_MEMORY_CACHE_SIZE) return
        
        // Remove oldest 20% of entries
        val entriesToRemove = memoryCache.entries
            .sortedBy { it.value.timestamp }
            .take(memoryCache.size / 5)
        
        entriesToRemove.forEach { entry ->
            memoryCache.remove(entry.key)
        }
    }
    
    /**
     * Cache statistics data class
     */
    data class CacheStats(
        val totalEntries: Int,
        val memoryUsageKB: Int
    )
}