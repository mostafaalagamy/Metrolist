package com.metrolist.music.translation.traditional

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Microsoft Translator provider
 */
object MicrosoftTranslatorProvider : TraditionalProvider {
    
    override suspend fun translate(text: String, sourceLangCode: String, targetLangCode: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=$sourceLangCode&to=$targetLangCode"
            
            val jsonPayload = """
                [{"Text": "$text"}]
            """.trimIndent()
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MetrolistMusicApp/1.0")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            
            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                return@withContext parseResponse(response) ?: text
            } else {
                throw Exception("Microsoft Translator API error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun parseResponse(response: String): String? {
        try {
            // Microsoft Translator response format: [{"translations":[{"text":"translated text"}]}]
            val regex = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(response)
            
            if (match != null) {
                var translation = match.groupValues[1]
                // Clean up escaped characters
                translation = translation.replace("\\\"", "\"")
                translation = translation.replace("\\n", "\n")
                translation = translation.replace("\\/", "/")
                return translation
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
}