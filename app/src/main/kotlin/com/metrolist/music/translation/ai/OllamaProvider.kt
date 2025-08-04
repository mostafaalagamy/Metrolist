package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ollama local AI translation provider
 */
object OllamaProvider : AIProvider {
    
    override suspend fun translate(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "http://localhost:11434/api/generate"
            
            val requestBody = """
                {
                    "model": "llama3.2",
                    "prompt": "$prompt",
                    "stream": false,
                    "options": {
                        "temperature": 0.7,
                        "top_p": 0.9
                    }
                }
            """.trimIndent()
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 45000
            
            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                return@withContext parseResponse(response) ?: originalText
            } else {
                throw Exception("Ollama local AI error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun parseResponse(response: String): String? {
        try {
            val regex = Regex("\"response\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
}