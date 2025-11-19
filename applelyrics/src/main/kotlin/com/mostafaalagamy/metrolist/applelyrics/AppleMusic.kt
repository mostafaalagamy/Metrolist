package com.mostafaalagamy.metrolist.applelyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.mostafaalagamy.metrolist.applelyrics.models.SearchResponse
import com.mostafaalagamy.metrolist.applelyrics.models.LyricsResponse

object AppleMusic {
    private val baseUrls = listOf(
        "http://lyrics.paxsenix.dpdns.org/",
        "https://paxsenix.alwaysdata.net/"
    )

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
                url(baseUrls.first())
            }
            expectSuccess = true
        }
    }

    private suspend inline fun <T> makeRequest(endpoint: String, crossinline block: suspend (HttpClient, URLBuilder) -> T): T? {
        for (baseUrl in baseUrls) {
            try {
                val urlBuilder = URLBuilder(baseUrl + endpoint)
                return block(httpClient, urlBuilder)
            } catch (e: Exception) {
                // Try the next base URL
            }
        }
        return null
    }

    suspend fun searchSong(songName: String, artistName: String): com.mostafaalagamy.metrolist.applelyrics.models.Track? {
        val searchResponse = makeRequest("searchAppleMusic.php") { client, urlBuilder ->
            urlBuilder.parameters.append("q", "$songName $artistName")
            client.get(urlBuilder.build()).body<SearchResponse>()
        }

        return searchResponse?.firstOrNull { it.songName.equals(songName, ignoreCase = true) }
    }

    suspend fun getLyrics(id: String): LyricsResponse? {
        return makeRequest("getAppleMusicLyrics.php") { client, urlBuilder ->
            urlBuilder.parameters.append("id", id)
            client.get(urlBuilder.build()).body<LyricsResponse>()
        }
    }
}
