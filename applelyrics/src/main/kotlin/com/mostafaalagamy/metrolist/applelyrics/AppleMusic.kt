package com.mostafaalagamy.metrolist.applelyrics

import com.mostafaalagamy.metrolist.applelyrics.models.LyricsResponse
import com.mostafaalagamy.metrolist.applelyrics.models.SearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AppleMusic {
    private val baseUrls = listOf(
        "http://lyrics.paxsenix.dpdns.org/",
        "https://paxsenix.alwaysdata.net/"
    )

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                    contentType = ContentType.Text.Html
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 5_000
            }
            defaultRequest {
                url(baseUrls.first())
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            expectSuccess = true
        }
    }

    private suspend inline fun <T> makeRequest(endpoint: String, crossinline block: suspend (HttpClient, URLBuilder) -> T): T? {
        for (baseUrl in baseUrls) {
            val urlBuilder = URLBuilder(baseUrl + endpoint)
            try {
                println("AppleMusic: Attempting request to ${urlBuilder.buildString()}")
                val response = block(httpClient, urlBuilder)
                println("AppleMusic: Request to ${urlBuilder.buildString()} successful")
                return response
            } catch (e: Exception) {
                println("AppleMusic: Failed request to ${urlBuilder.buildString()}: $e")
            }
        }
        println("AppleMusic: All requests failed for endpoint $endpoint")
        return null
    }

    suspend fun searchSong(songName: String, artistName: String): com.mostafaalagamy.metrolist.applelyrics.models.Track? {
        val searchResponse = makeRequest("searchAppleMusic.php") { client, urlBuilder ->
            urlBuilder.parameters.append("q", "$songName $artistName")
            client.get(urlBuilder.build()).body<SearchResponse>()
        }

        return searchResponse
            ?.sortedByDescending { it.songName.length }
            ?.firstOrNull {
            (songName.contains(it.songName, ignoreCase = true) ||
                    it.songName.contains(songName, ignoreCase = true)) &&
                    (artistName.contains(it.artistName, ignoreCase = true) ||
                            it.artistName.contains(artistName, ignoreCase = true))
        }
    }

    suspend fun getLyrics(id: String): String? {
        return makeRequest("getAppleMusicLyrics.php") { client, urlBuilder ->
            urlBuilder.parameters.append("id", id)
            client.get(urlBuilder.build()).body<String>()
        }
    }
}
