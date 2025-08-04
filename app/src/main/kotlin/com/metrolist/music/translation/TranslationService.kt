package com.metrolist.music.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    /**
     * Main translation function with intelligent service selection
     */
    suspend fun translateLyricsWithAI(text: String, targetLanguage: String = "English"): String = withContext(Dispatchers.IO) {
        try {
            // Skip translation for very short or non-translatable text
            if (!shouldTranslate(text)) {
                return@withContext text
            }
            
            // Clean and prepare text
            val cleanedText = preprocessText(text)
            val sourceLang = languageDetector.detectLanguage(cleanedText)
            
            // Don't translate if source and target are the same
            if (languageDetector.isSameLanguage(sourceLang, targetLanguage)) {
                return@withContext text
            }
            
            // Try AI translation first (best quality)
            try {
                val aiResult = aiTranslationService.translate(cleanedText, sourceLang, targetLanguage)
                if (isValidTranslation(aiResult, text)) {
                    return@withContext postProcessor.process(aiResult, targetLanguage)
                }
            } catch (e: Exception) {
                // Fall back to traditional services
            }
            
            // Try traditional translation services
            val traditionalResult = traditionalTranslationService.translate(cleanedText, sourceLang, targetLanguage)
            if (isValidTranslation(traditionalResult, text)) {
                return@withContext postProcessor.process(traditionalResult, targetLanguage)
            }
            
            // Return original text if all methods fail
            return@withContext text
            
        } catch (e: Exception) {
            return@withContext text
        }
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
            "Arabic" to "🇪🇬",
            "English" to "🇺🇸",
            "French" to "🇫🇷", 
            "German" to "🇩🇪",
            "Spanish" to "🇪🇸",
            "Italian" to "🇮🇹",
            "Portuguese" to "🇵🇹",
            "Russian" to "🇷🇺",
            "Chinese" to "🇨🇳",
            "Japanese" to "🇯🇵",
            "Korean" to "🇰🇷"
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