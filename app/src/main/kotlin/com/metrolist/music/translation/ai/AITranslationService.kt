package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI-powered translation service using multiple AI providers
 */
object AITranslationService {
    
    private val providers = listOf(
        OpenAIProvider,              // Best quality for lyrics translation
        GroqAIProvider,             // Fast and reliable fallback
        HuggingFaceProvider,        // Multiple model options
        OllamaProvider              // Local processing option
    )
    
    /**
     * Translate text using AI with context understanding
     */
    suspend fun translate(text: String, sourceLang: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val prompt = buildLyricsTranslationPrompt(text, sourceLang, targetLanguage)
        
        for (provider in providers) {
            try {
                val result = provider.translate(prompt, text)
                if (isValidAIResult(result, text)) {
                    return@withContext result
                }
            } catch (e: Exception) {
                // Try next provider
                continue
            }
        }
        
        throw Exception("All AI providers failed")
    }
    
    /**
     * Build intelligent prompt for song lyrics translation
     */
    private fun buildLyricsTranslationPrompt(text: String, sourceLang: String, targetLanguage: String): String {
        return """
            You are an expert song lyrics translator with deep understanding of music, poetry, and cultural nuances.
            
            Task: Translate these song lyrics from $sourceLang to $targetLanguage
            
            Critical Requirements:
            - Capture the emotional essence, not just literal meaning
            - Keep the poetic flow and musical rhythm
            - Use contemporary, natural $targetLanguage expressions
            - Consider cultural context and adapt metaphors appropriately
            - Make it singable and meaningful to native $targetLanguage speakers
            - Preserve names, places, and artistic expressions
            - Adapt universal sounds (oh, ah, wow) to $targetLanguage equivalents
            
            Original: "$text"
            
            Return ONLY the $targetLanguage translation (no quotes, explanations, or extra text):
        """.trimIndent()
    }
    
    /**
     * Validate AI translation result
     */
    private fun isValidAIResult(result: String, original: String): Boolean {
        val cleanResult = result.trim()
        
        // Basic validations
        if (cleanResult.isBlank() || cleanResult == original) return false
        
        // Reject error messages
        val errorPhrases = listOf(
            "Error", "error", "I cannot", "I can't", "I'm sorry", "Sorry", "unable to",
            "translation:", "translate:", "result:", "output:", "Note:", "Please note",
            "لا أستطيع", "أعتذر", "عذراً", "خطأ"
        )
        
        if (errorPhrases.any { cleanResult.startsWith(it, ignoreCase = true) }) return false
        
        // Reject if it's just the original text repeated
        if (cleanResult.equals(original, ignoreCase = true)) return false
        
        // Reject if it contains too many non-letter characters (likely formatting issues)
        val letterCount = cleanResult.count { it.isLetter() }
        if (letterCount < cleanResult.length * 0.3) return false
        
        // Reject if it's suspiciously long compared to original
        if (cleanResult.length > original.length * 3) return false
        
        return true
    }
}