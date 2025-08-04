package com.metrolist.music.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.metrolist.music.translation.ai.AITranslationService
import com.metrolist.music.translation.traditional.TraditionalTranslationService
import com.metrolist.music.translation.utils.LanguageDetector
import com.metrolist.music.translation.utils.TranslationPostProcessor

/**
 * Main translation service that coordinates all translation methods
 * Provides a clean interface for the rest of the application
 */
object TranslationService {
    
    private val aiTranslationService = AITranslationService
    private val traditionalTranslationService = TraditionalTranslationService
    private val languageDetector = LanguageDetector
    private val postProcessor = TranslationPostProcessor
    
    // Timeout for translations to prevent UI freezing
    private const val TRANSLATION_TIMEOUT_MS = 30000L // 30 seconds
    
    /**
     * Main translation function with intelligent service selection and timeout
     */
    suspend fun translateLyricsWithAI(text: String, targetLanguage: String = "English"): String = withContext(Dispatchers.IO) {
        // Use timeout to prevent long-running translations from blocking UI
        withTimeoutOrNull(TRANSLATION_TIMEOUT_MS) {
            try {
                // Skip translation for very short or non-translatable text
                if (!shouldTranslate(text)) {
                    return@withTimeoutOrNull text
                }
                
                // Clean and prepare text
                val cleanedText = preprocessText(text)
                val sourceLang = languageDetector.detectLanguage(cleanedText)
                
                // Don't translate if source and target are the same
                if (languageDetector.isSameLanguage(sourceLang, targetLanguage)) {
                    return@withTimeoutOrNull text
                }
                
                // Skip very short or meaningless text
                if (shouldSkipTranslation(cleanedText)) {
                    return@withTimeoutOrNull text
                }
                
                // Try AI translation first (best quality) with shorter timeout
                try {
                    withTimeoutOrNull(15000L) { // 15 seconds for AI
                        val aiResult = aiTranslationService.translate(cleanedText, sourceLang, targetLanguage)
                        if (isValidTranslation(aiResult, text)) {
                            return@withTimeoutOrNull postProcessor.process(aiResult, targetLanguage)
                        }
                    }
                } catch (e: Exception) {
                    // Fall back to traditional services
                }
                
                // Try traditional translation services with shorter timeout
                try {
                    withTimeoutOrNull(10000L) { // 10 seconds for traditional
                        val traditionalResult = traditionalTranslationService.translate(cleanedText, sourceLang, targetLanguage)
                        if (isValidTranslation(traditionalResult, text)) {
                            return@withTimeoutOrNull postProcessor.process(traditionalResult, targetLanguage)
                        }
                    }
                } catch (e: Exception) {
                    // All services failed, will return original text
                }
                
                // Return original text if all methods fail
                return@withTimeoutOrNull text
                
            } catch (e: Exception) {
                return@withTimeoutOrNull text
            }
        } ?: text // Return original text if timeout occurs
    }
    
    /**
     * Check if text should be translated
     */
    private fun shouldTranslate(text: String): Boolean {
        val cleanText = text.trim()
        
        // Skip very short text
        if (cleanText.length < 2) return false
        
        // Skip common musical interjections
        val universalSounds = listOf("oh", "ah", "eh", "mm", "hmm", "la", "na", "da", "ya", "وو", "آه", "أوه", "لا", "نا", "يا")
        if (universalSounds.contains(cleanText.lowercase())) return false
        
        // Skip if text is mostly punctuation or numbers
        val alphaCount = cleanText.count { it.isLetter() }
        if (alphaCount < cleanText.length * 0.5) return false
        
        return true
    }
    
    /**
     * Preprocess text for better translation
     */
    private fun preprocessText(text: String): String {
        var processedText = text.trim()
        
        // Remove excessive punctuation
        processedText = processedText.replace(Regex("[.]{2,}"), "...")
        processedText = processedText.replace(Regex("[!]{2,}"), "!")
        processedText = processedText.replace(Regex("[?]{2,}"), "?")
        
        // Handle common lyrical repetitions
        processedText = processedText.replace(Regex("\\b(\\w+)\\s+\\1\\b"), "$1")
        
        return processedText
    }
    
    /**
     * Check if translation should be skipped for very short or meaningless text
     */
    private fun shouldSkipTranslation(text: String): Boolean {
        val cleanText = text.trim()
        
        // Skip very short text (less than 3 characters)
        if (cleanText.length < 3) return true
        
        // Skip text that's mostly punctuation or symbols
        val letterCount = cleanText.count { it.isLetter() }
        if (letterCount < 2) return true
        
        // Skip common interjections and sounds
        val commonSounds = setOf(
            "oh", "ah", "eh", "mm", "hmm", "la", "na", "da", "ya", "hey", "hi", "yo", "ok", "wow",
            "وو", "آه", "أوه", "لا", "نا", "يا", "هاي", "أوكيه"
        )
        if (commonSounds.contains(cleanText.lowercase())) return true
        
        // Skip repeated single characters
        if (Regex("^(.)\\1*$").matches(cleanText)) return true
        
        return false
    }
    
    /**
     * Validate translation result
     */
    private fun isValidTranslation(result: String, original: String): Boolean {
        return result.isNotBlank() && 
               result != original && 
               !result.startsWith("Error") &&
               !result.startsWith("Failed")
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "Arabic" to "AR",
            "English" to "EN",
            "French" to "FR", 
            "German" to "DE",
            "Spanish" to "ES",
            "Italian" to "IT",
            "Portuguese" to "PT",
            "Russian" to "RU",
            "Chinese" to "ZH",
            "Japanese" to "JA",
            "Korean" to "KO"
        )
    }
    
    /**
     * Check if text needs translation based on language detection
     */
    fun needsTranslation(text: String, targetLanguage: String): Boolean {
        if (!shouldTranslate(text)) return false
        
        val sourceLang = languageDetector.detectLanguage(text)
        return !languageDetector.isSameLanguage(sourceLang, targetLanguage)
    }
}