package com.metrolist.music.translation.traditional

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Traditional translation service using established APIs
 */
object TraditionalTranslationService {
    
    private val providers = listOf(
        LibreTranslateProvider,
        MicrosoftTranslatorProvider,
        GoogleTranslateProvider
    )
    
    /**
     * Translate using traditional services
     */
    suspend fun translate(text: String, sourceLang: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val sourceLangCode = getLanguageCode(sourceLang)
        val targetLangCode = getLanguageCode(targetLanguage)
        
        for (provider in providers) {
            try {
                val result = provider.translate(text, sourceLangCode, targetLangCode)
                if (isValidResult(result, text)) {
                    return@withContext result
                }
            } catch (e: Exception) {
                // Try next provider
                continue
            }
        }
        
        throw Exception("All traditional services failed")
    }
    
    /**
     * Convert language name to language code
     */
    private fun getLanguageCode(language: String): String {
        return when (language) {
            "Arabic" -> "ar"
            "English" -> "en"
            "French" -> "fr"
            "German" -> "de"
            "Spanish" -> "es"
            "Italian" -> "it"
            "Portuguese" -> "pt"
            "Russian" -> "ru"
            "Chinese" -> "zh"
            "Japanese" -> "ja"
            "Korean" -> "ko"
            else -> "auto"
        }
    }
    
    /**
     * Validate translation result
     */
    private fun isValidResult(result: String, original: String): Boolean {
        return result.isNotBlank() && 
               result != original && 
               !result.startsWith("Error")
    }
}