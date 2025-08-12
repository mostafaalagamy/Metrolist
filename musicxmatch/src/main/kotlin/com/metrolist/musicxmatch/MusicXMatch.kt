package com.metrolist.musicxmatch

import com.metrolist.musicxmatch.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json

object MusicXMatch {
    private val client by lazy {
        HttpClient(CIO) {
            expectSuccess = false // MusicXMatch might return error codes
            
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                })
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 30000
            }
            
            defaultRequest {
                url("https://www.musixmatch.com/ws/1.1/")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
            }
        }
    }

    private suspend fun searchTracks(
        query: String,
        page: Int = 1
    ): Result<List<Track>> = runCatching {
        currentCoroutineContext().ensureActive()
        
        val response = client.get("track.search") {
            parameter("app_id", "web-desktop-app-v1.0")
            parameter("format", "json")
            parameter("q", query)
            parameter("f_has_lyrics", "true")
            parameter("page_size", "100")
            parameter("page", page.toString())
        }

        val musicXMatchResponse = response.body<MusicXMatchResponse<TrackSearchBody>>()
        
        if (musicXMatchResponse.message.header.statusCode != 200) {
            throw Exception("MusicXMatch API error: ${musicXMatchResponse.message.header.statusCode}")
        }
        
        musicXMatchResponse.message.body.trackList.map { it.track }
    }

    private suspend fun getTrackLyrics(trackId: Int): Result<String> = runCatching {
        currentCoroutineContext().ensureActive()
        
        val response = client.get("track.lyrics.get") {
            parameter("app_id", "web-desktop-app-v1.0")
            parameter("format", "json")
            parameter("track_id", trackId.toString())
        }

        val musicXMatchResponse = response.body<MusicXMatchResponse<LyricsBody>>()
        
        if (musicXMatchResponse.message.header.statusCode != 200) {
            throw Exception("MusicXMatch API error: ${musicXMatchResponse.message.header.statusCode}")
        }
        
        val lyrics = musicXMatchResponse.message.body.lyrics?.lyricsBody
            ?: throw Exception("No lyrics found for track ID: $trackId")
        
        // Remove MusicXMatch restrictions text if present
        lyrics.replace("******* This Lyrics is NOT for Commercial use *******", "")
              .replace("(1409617635700)", "")
              .trim()
    }

    private suspend fun getTrackRichSync(trackId: Int): Result<String> = runCatching {
        currentCoroutineContext().ensureActive()
        
        val response = client.get("track.richsync.get") {
            parameter("app_id", "web-desktop-app-v1.0")
            parameter("format", "json")
            parameter("track_id", trackId.toString())
        }

        val musicXMatchResponse = response.body<MusicXMatchResponse<RichSyncBody>>()
        
        if (musicXMatchResponse.message.header.statusCode != 200) {
            throw Exception("MusicXMatch API error: ${musicXMatchResponse.message.header.statusCode}")
        }
        
        musicXMatchResponse.message.body.richsync?.richsyncBody
            ?: throw Exception("No rich sync found for track ID: $trackId")
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = runCatching {
        currentCoroutineContext().ensureActive()
        
        val query = "$artist $title".trim()
        val tracks = searchTracks(query).getOrThrow()
        
        if (tracks.isEmpty()) {
            throw Exception("No tracks found for: $title by $artist")
        }
        
        val bestMatch = tracks.bestMatchingFor(title, artist, duration)
            ?: throw Exception("No suitable match found")
        
        // Try to get rich sync first (synced lyrics), then fallback to regular lyrics
        val richSyncResult = getTrackRichSync(bestMatch.trackId)
        if (richSyncResult.isSuccess) {
            return@runCatching richSyncResult.getOrThrow()
        }
        
        // Fallback to regular lyrics
        getTrackLyrics(bestMatch.trackId).getOrThrow()
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        try {
            val result = getLyrics(title, artist, duration)
            result.getOrNull()?.let { lyrics ->
                callback(lyrics)
            }
        } catch (e: Exception) {
            // Silently handle errors as per callback pattern
        }
    }
}