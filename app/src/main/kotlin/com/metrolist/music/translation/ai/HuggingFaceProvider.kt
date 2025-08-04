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
            // Try multiple high-quality translation models
            val models = listOf(
                "facebook/nllb-200-distilled-600M",  // Better multilingual model
                "Helsinki-NLP/opus-mt-mul-en"        // Fallback for English
            )
            
            for (model in models) {
                try {
                    val url = "https://api-inference.huggingface.co/models/$model"
                    
                    val requestBody = """
                        {
                            "inputs": "$originalText",
                            "parameters": {
                                "max_length": 200,
                                "temperature": 0.1,
                                "do_sample": false,
                                "early_stopping": true,
                                "repetition_penalty": 1.05
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
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000
                    
                    connection.outputStream.use { os ->
                        val input = requestBody.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val result = parseResponse(response)
                        if (result != null && result != originalText) {
                            return@withContext result
                        }
                    }
                } catch (e: Exception) {
                    // Try next model
                    continue
                }
            }
            
            throw Exception("All Hugging Face models failed")
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