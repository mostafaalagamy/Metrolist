package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import com.metrolist.music.translation.TranslationConfig

/**
 * OpenAI GPT-4 provider for high-quality song lyrics translation
 * Based on community best practices and research findings
 */
object OpenAIProvider : AIProvider {
    
    override suspend fun translate(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            // Check if OpenAI is configured
            if (!TranslationConfig.isOpenAIConfigured()) {
                throw Exception("OpenAI API key not configured")
            }
            
            val url = "https://api.openai.com/v1/chat/completions"
            
            // Build specialized prompt for song lyrics translation
            val systemMessage = buildLyricsTranslationSystemPrompt()
            val userMessage = buildUserPrompt(originalText, extractTargetLanguage(prompt))
            
            val requestBody = JSONObject().apply {
                put("model", TranslationConfig.OPENAI_MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemMessage)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("max_tokens", TranslationConfig.OPENAI_MAX_TOKENS)
                put("temperature", TranslationConfig.OPENAI_TEMPERATURE)
                put("top_p", 0.9)
                put("frequency_penalty", 0.1)
                put("presence_penalty", 0.1)
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${TranslationConfig.OPENAI_API_KEY}")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            connection.doOutput = true
            connection.connectTimeout = TranslationConfig.CONNECTION_TIMEOUT
            connection.readTimeout = TranslationConfig.READ_TIMEOUT
            
            connection.outputStream.use { os ->
                val input = requestBody.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                return@withContext parseOpenAIResponse(response) ?: originalText
            } else {
                throw Exception("OpenAI API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Build specialized system prompt for song lyrics translation
     * Based on research from REFFLY paper and community best practices
     */
    private fun buildLyricsTranslationSystemPrompt(): String {
        return """
            You are a world-class song lyrics translator specializing in preserving musical and poetic essence across languages.
            
            Your expertise includes:
            - Deep understanding of musical context and rhythm
            - Cultural adaptation while preserving emotional core
            - Poetic translation that maintains singability
            - Contemporary, natural language expression
            - Musical terminology and lyrical conventions
            
            Translation Principles:
            1. EMOTIONAL FIDELITY: Capture the feeling, not just literal meaning
            2. MUSICAL FLOW: Ensure the translation sounds natural when sung
            3. CULTURAL ADAPTATION: Adapt metaphors and references appropriately
            4. CONTEMPORARY LANGUAGE: Use modern, relatable expressions
            5. ARTISTIC INTEGRITY: Preserve the song's artistic intent
            
            Avoid:
            - Literal, word-for-word translations
            - Awkward phrasing that breaks musical flow
            - Cultural references that don't translate
            - Archaic or overly formal language
            - Technical or academic terminology
            
            Always respond with ONLY the translated lyrics, no explanations or additional text.
        """.trimIndent()
    }
    
    /**
     * Build user prompt with context
     */
    private fun buildUserPrompt(text: String, targetLanguage: String): String {
        return """
            Translate this song lyric line to $targetLanguage:
            
            "$text"
            
            Requirements:
            - Keep the emotional tone and musical feeling
            - Use natural, contemporary $targetLanguage
            - Make it singable and poetic
            - Adapt any cultural references appropriately
            - Preserve names, places, and artistic expressions where appropriate
            
            Return only the translated lyric line:
        """.trimIndent()
    }
    
    /**
     * Extract target language from original prompt
     */
    private fun extractTargetLanguage(prompt: String): String {
        return when {
            prompt.contains("Arabic", ignoreCase = true) -> "Arabic"
            prompt.contains("English", ignoreCase = true) -> "English"
            prompt.contains("French", ignoreCase = true) -> "French"
            prompt.contains("Spanish", ignoreCase = true) -> "Spanish"
            prompt.contains("German", ignoreCase = true) -> "German"
            prompt.contains("Italian", ignoreCase = true) -> "Italian"
            prompt.contains("Portuguese", ignoreCase = true) -> "Portuguese"
            prompt.contains("Russian", ignoreCase = true) -> "Russian"
            prompt.contains("Chinese", ignoreCase = true) -> "Chinese"
            prompt.contains("Japanese", ignoreCase = true) -> "Japanese"
            prompt.contains("Korean", ignoreCase = true) -> "Korean"
            else -> "English"
        }
    }
    
    /**
     * Parse OpenAI API response
     */
    private fun parseOpenAIResponse(response: String): String? {
        try {
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                val content = message.getString("content")
                return content.trim()
            }
        } catch (e: Exception) {
            // Invalid JSON or unexpected structure
        }
        return null
    }
}