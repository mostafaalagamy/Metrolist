package com.metrolist.music.translation

/**
 * Configuration object for translation services
 * Replace with actual API keys when available
 */
object TranslationConfig {
    
    // OpenAI Configuration
    const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY_HERE"
    const val OPENAI_MODEL = "gpt-4o"
    const val OPENAI_MAX_TOKENS = 300
    const val OPENAI_TEMPERATURE = 0.1
    
    // Groq AI Configuration
    const val GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE"
    const val GROQ_MODEL = "llama-3.1-70b-versatile"
    
    // Hugging Face Configuration
    const val HUGGINGFACE_API_KEY = "YOUR_HUGGINGFACE_API_KEY_HERE"
    
    // Translation Settings
    const val MAX_TRANSLATION_LENGTH = 500
    const val CONNECTION_TIMEOUT = 30000
    const val READ_TIMEOUT = 30000
    
    // Language Mappings
    val LANGUAGE_CODES = mapOf(
        "Arabic" to "ar",
        "English" to "en", 
        "French" to "fr",
        "German" to "de",
        "Spanish" to "es",
        "Italian" to "it",
        "Portuguese" to "pt",
        "Russian" to "ru",
        "Chinese" to "zh",
        "Japanese" to "ja",
        "Korean" to "ko"
    )
    
    /**
     * Get language code for a language name
     */
    fun getLanguageCode(language: String): String {
        return LANGUAGE_CODES[language] ?: "auto"
    }
    
    /**
     * Check if OpenAI API is configured
     */
    fun isOpenAIConfigured(): Boolean {
        return OPENAI_API_KEY != "YOUR_OPENAI_API_KEY_HERE" && OPENAI_API_KEY.isNotBlank()
    }
    
    /**
     * Check if Groq AI is configured
     */
    fun isGroqConfigured(): Boolean {
        return GROQ_API_KEY != "YOUR_GROQ_API_KEY_HERE" && GROQ_API_KEY.isNotBlank()
    }
    
    /**
     * Check if Hugging Face is configured
     */
    fun isHuggingFaceConfigured(): Boolean {
        return HUGGINGFACE_API_KEY != "YOUR_HUGGINGFACE_API_KEY_HERE" && HUGGINGFACE_API_KEY.isNotBlank()
    }
}