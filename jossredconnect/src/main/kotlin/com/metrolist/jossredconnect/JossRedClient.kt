package com.metrolist.jossredconnect

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object JossRedClient {
    private const val BASE_URL_API = "https://jossred.josprox.com/api/"
    private const val BASE_STREAM_URL = "https://jossred.josprox.com/yt/v2/stream/"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Custom exception for JossRed errors
    class JossRedException(
        val statusCode: Int,
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    // Method to get the streaming URL with error handling
    fun getStreamingUrl(mediaId: String): String {
        val requestUrl = "$BASE_STREAM_URL$mediaId"
        val request = Request.Builder()
            .url(requestUrl)
            .head() // We use HEAD to verify without downloading the full content
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            throw JossRedException(
                statusCode = -1,
                message = "Error de conexión: ${e.message}",
                cause = e
            )
        }

        if (!response.isSuccessful) {
            response.close()
            throw JossRedException(
                statusCode = response.code,
                message = when (response.code) {
                    403 -> "Acceso denegado (403) para el recurso"
                    404 -> "Recurso no encontrado (404)"
                    in 400..499 -> "Error del cliente (${response.code})"
                    in 500..599 -> "Error del servidor (${response.code})"
                    else -> "Error desconocido (${response.code})"
                }
            )
        }

        response.close()
        return requestUrl
    }
}
