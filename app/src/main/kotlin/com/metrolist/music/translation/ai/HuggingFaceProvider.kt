package com.metrolist.music.translation.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Hugging Face AI translation provider
 */
object HuggingFaceProvider : AIProvider {
    
    override suspend fun translate(prompt: String, originalText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api-inference.huggingface.co/models/facebook/mbart-large-50-many-to-many-mmt"
            
            val requestBody = """
                {
                    "inputs": "$prompt",
                    "parameters": {
                        "max_length": 200,
                        "temperature": 0.7,
                        "do_sample": true
                    }
                }
            """.trimIndent()
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            // Note: For production, add your Hugging Face API key
            // connection.setRequestProperty("Authorization", "Bearer YOUR_HF_TOKEN")
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
                throw Exception("Hugging Face API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun parseResponse(response: String): String? {
        try {
            val regex = Regex("\"generated_text\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            return match?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }
}