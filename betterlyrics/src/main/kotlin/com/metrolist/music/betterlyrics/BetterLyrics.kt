package com.metrolist.music.betterlyrics

import com.metrolist.music.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api.boidu.dev")
            }

            expectSuccess = false
        }
    }

    private fun normalizeTitle(title: String): String {
        return title
            .replace(Regex("\\s*\\(.*?\\)\\s*"), " ")
            .replace(Regex("\\s*\\[.*?\\]\\s*"), " ")
            .replace(Regex("\\s*-\\s*(Official|Music|Video|Audio|Lyrics|HD|HQ|4K|Remaster|Remastered|Live|Acoustic|Remix|Mix|Version|Edit|Extended|Radio|Single|Album|feat\\.|ft\\.).*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeArtist(artist: String): String {
        return artist
            .replace(", & ", ", ")
            .replace(" & ", ", ")
            .replace(" x ", ", ")
            .replace(" X ", ", ")
            .replace(Regex("\\s*\\(.*?\\)\\s*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractMainArtist(artist: String): String {
        val normalized = normalizeArtist(artist)
        return normalized.split(",", "&", "feat.", "ft.", "Feat.", "Ft.")
            .firstOrNull()?.trim() ?: normalized
    }

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        duration: Int = -1,
        album: String? = null,
    ): String? = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) {
                parameter("d", duration)
            }
            if (!album.isNullOrBlank()) {
                parameter("al", album)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<TTMLResponse>().ttml
        } else {
            null
        }
    }.getOrNull()

    private suspend fun tryFetchWithStrategies(
        title: String,
        artist: String,
        duration: Int,
    ): String? {
        // Strategy 1: Try with original values
        fetchTTML(artist, title, duration)?.let { return it }

        // Strategy 2: Try with normalized title and artist
        val normalizedTitle = normalizeTitle(title)
        val normalizedArtist = normalizeArtist(artist)
        if (normalizedTitle != title || normalizedArtist != artist) {
            fetchTTML(normalizedArtist, normalizedTitle, duration)?.let { return it }
        }

        // Strategy 3: Try with main artist only
        val mainArtist = extractMainArtist(artist)
        if (mainArtist != normalizedArtist) {
            fetchTTML(mainArtist, normalizedTitle, duration)?.let { return it }
        }

        // Strategy 4: Try without duration (more flexible matching)
        fetchTTML(normalizedArtist, normalizedTitle, -1)?.let { return it }

        // Strategy 5: Try with main artist and no duration
        if (mainArtist != normalizedArtist) {
            fetchTTML(mainArtist, normalizedTitle, -1)?.let { return it }
        }

        return null
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
    ) = runCatching {
        val ttml = tryFetchWithStrategies(title, artist, duration)
            ?: throw IllegalStateException("Lyrics unavailable")
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        TTMLParser.toLRC(parsedLines)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }
}
