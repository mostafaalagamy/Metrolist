package com.metrolist.music.translation.traditional

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Enhanced Google Translate provider using web service
 */
object GoogleTranslateProvider : TraditionalProvider {
    
    override suspend fun translate(text: String, sourceLangCode: String, targetLangCode: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val googleSourceCode = getGoogleLanguageCode(sourceLangCode)
            val googleTargetCode = getGoogleLanguageCode(targetLangCode)
            
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$googleSourceCode&tl=$googleTargetCode&dt=t&q=$encodedText"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                return@withContext parseResponse(response) ?: text
            } else {
                throw Exception("Google Translate error: $responseCode")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun parseResponse(response: String): String? {
        try {
            // Google Translate returns format: [[["translated text","original text",null,null,10]],null,"en",null,null,null,null,[]]
            val regex = Regex("\\[\\[\\[\"([^\"]+)\"")
            val match = regex.find(response)
            
            if (match != null) {
                var translation = match.groupValues[1]
                // Clean up escaped characters
                translation = translation.replace("\\\"", "\"")
                translation = translation.replace("\\n", "\n")
                translation = translation.replace("\\/", "/")
                translation = translation.replace("\\\\", "\\")
                return translation
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun getGoogleLanguageCode(langCode: String): String {
        return when (langCode) {
            "ar" -> "ar"
            "en" -> "en"
            "fr" -> "fr"
            "de" -> "de"
            "es" -> "es"
            "it" -> "it"
            "pt" -> "pt"
            "ru" -> "ru"
            "zh" -> "zh-cn"
            "ja" -> "ja"
            "ko" -> "ko"
            "auto" -> "auto"
            else -> "en"
        }
    }
}