package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI-powered translation service using multiple AI providers
 */
object AITranslationService {
    
    private val providers = listOf(
        HuggingFaceProvider,
        GroqAIProvider,
        OllamaProvider
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
            You are a professional translator specializing in song lyrics and poetry. 
            
            Task: Translate the following song lyrics from $sourceLang to $targetLanguage.
            
            Instructions:
            - Preserve the emotional meaning and poetic essence
            - Maintain the rhythm and flow where possible
            - Consider cultural context and metaphors
            - Use natural, contemporary language
            - Avoid literal word-for-word translation
            - If it's a name, place, or proper noun, keep it as is
            - If it's a universal expression (oh, ah, wow), adapt appropriately
            
            Original lyrics: "$text"
            
            Provide ONLY the translated text without any explanations:
        """.trimIndent()
    }
    
    /**
     * Validate AI translation result
     */
    private fun isValidAIResult(result: String, original: String): Boolean {
        return result.isNotBlank() && 
               result != original && 
               !result.startsWith("Error") &&
               !result.startsWith("I cannot") &&
               !result.startsWith("I'm sorry") &&
               !result.contains("translation:")
    }
}