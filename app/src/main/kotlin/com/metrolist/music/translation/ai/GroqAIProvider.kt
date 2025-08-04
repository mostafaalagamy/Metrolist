package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq AI translation provider (very fast and free)
 */
object GroqAIProvider : AIProvider {
    
    override suspend fun translate(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.groq.com/openai/v1/chat/completions"
            
            val requestBody = """
                {
                    "model": "llama3-8b-8192",
                    "messages": [
                        {
                            "role": "user",
                            "content": "$prompt"
                        }
                    ],
                    "temperature": 0.7,
                    "max_tokens": 150
                }
            """.trimIndent()
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            // Note: For production, add your Groq API key
            // connection.setRequestProperty("Authorization", "Bearer YOUR_GROQ_API_KEY")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                return@withContext parseResponse(response) ?: originalText
            } else {
                throw Exception("Groq AI error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun parseResponse(response: String): String? {
        try {
            val regex = Regex("\"content\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
}