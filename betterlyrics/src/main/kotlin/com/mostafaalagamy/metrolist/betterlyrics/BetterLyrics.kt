package com.mostafaalagamy.metrolist.betterlyrics

import com.mostafaalagamy.metrolist.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            defaultRequest {
                url("https://lyrics-api-go-better-lyrics-api-pr-12.up.railway.app")
            }
            expectSuccess = true
        }
    }

    private suspend fun fetchTTML(artist: String, title: String, duration: Int): String? {
        return try {
            httpClient.get("/getLyrics") {
                parameter("s", title)
                parameter("a", artist)
                if (duration > 0) {
                    parameter("d", duration)
                }
            }.body<TTMLResponse>().ttml
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLyrics(title: String, artist: String, duration: Int): Result<String> {
        return runCatching {
            val ttml = fetchTTML(artist, title, duration)
                ?: return@runCatching "No lyrics found"

            val parsedLines = TTMLParser.parseTTML(ttml)
            if (parsedLines.isEmpty()) {
                return@runCatching "No lyrics found"
            }

            TTMLParser.toLRC(parsedLines)
        }
    }

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        duration: Int,
        onSuccess: (String) -> Unit
    ) {
        getLyrics(title, artist, duration).onSuccess(onSuccess)
    }
}
