package com.metrolist.music.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Test translation provider that always works
 * Used for debugging translation issues
 */
object TestTranslationProvider {
    
    suspend fun translateForTesting(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        // Simple test translation that always works
        return@withContext when (targetLanguage.lowercase()) {
            "arabic" -> "[$targetLanguage] $text"
            "english" -> "[$targetLanguage] $text"
            "french" -> "[$targetLanguage] $text"
            "spanish" -> "[$targetLanguage] $text"
            "german" -> "[$targetLanguage] $text"
            else -> "[$targetLanguage] $text"
        }
    }
}