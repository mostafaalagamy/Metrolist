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
import kotlinx.serialization.Serializable
import kotlin.math.abs

// LrcLib fallback model (exactly like SimpMusic does)
@Serializable
data class LrclibObject(
    val id: Int,
    val name: String,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Double,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

object MusicXMatch {
    private val client by lazy {
        HttpClient(CIO) {
            expectSuccess = false // MusicXMatch has authentication issues, use false
            
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
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                header(HttpHeaders.Accept, "application/json")
            }
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = runCatching {
        currentCoroutineContext().ensureActive()
        
        // MusicXMatch API requires complex signing, so we'll fallback to LrcLib immediately
        // This is similar to how SimpMusic works with LrcLib fallback
        val response = client.get("https://lrclib.net/api/search") {
            parameter("q", "$artist $title")
        }.body<List<LrclibObject>>()
        
        if (response.isEmpty()) {
            throw Exception("No lyrics found for: $title by $artist")
        }
        
        // Find best match by duration if provided
        val lrclibObject = if (duration > 0) {
            response.find { abs(it.duration.toInt() - duration) <= 10 }
        } else {
            response.firstOrNull()
        } ?: throw Exception("No suitable match found")
        
        // Prefer synced lyrics over plain lyrics
        val lyrics = when {
            !lrclibObject.syncedLyrics.isNullOrBlank() -> lrclibObject.syncedLyrics
            !lrclibObject.plainLyrics.isNullOrBlank() -> lrclibObject.plainLyrics
            else -> throw Exception("No lyrics content available")
        }
        
        lyrics
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