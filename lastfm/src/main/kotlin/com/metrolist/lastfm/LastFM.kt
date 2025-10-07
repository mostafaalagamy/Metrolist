package com.metrolist.lastfm

import com.metrolist.lastfm.models.Authentication
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object LastFM {
    var sessionKey: String? = null

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
            defaultRequest { url("https://ws.audioscrobbler.com/2.0/") }
            expectSuccess = true
        }
    }

    private fun Map<String, String>.apiSig(secret: String): String {
        val sorted = toSortedMap()
        val toHash = sorted.entries.joinToString("") { it.key + it.value } + secret
        val digest = MessageDigest.getInstance("MD5").digest(toHash.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun HttpRequestBuilder.lastfmParams(
        method: String,
        apiKey: String,
        secret: String,
        sessionKey: String? = null,
        extra: Map<String, String> = emptyMap()
    ) {
        contentType(ContentType.Application.FormUrlEncoded)
        userAgent("Metrolist (https://github.com/mostafaalagamy/Metrolist)")
        val params = mutableMapOf(
            "method" to method,
            "api_key" to apiKey
        ).apply {
            sessionKey?.let { put("sk", it) }
            putAll(extra)
        }
        params["api_sig"] = params.apiSig(secret)
        params.forEach { (k, v) -> parameter(k, v) }
    }

    // TODO: Change this to OAuth
    suspend fun getMobileSession(username: String, password: String) = runCatching {
        client.post {
            lastfmParams(
                method = "auth.getMobileSession",
                apiKey = API_KEY,
                secret = SECRET,
                extra = mapOf("username" to username, "password" to password)
            )
            parameter("format", "json")
        }.body<Authentication>()
    }

    suspend fun updateNowPlaying(
        artist: String, track: String,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null
    ) = runCatching {
        client.post {
            lastfmParams(
                method = "track.updateNowPlaying",
                apiKey = API_KEY,
                secret = SECRET,
                sessionKey = sessionKey!!,
                extra = buildMap {
                    put("artist", artist)
                    put("track", track)
                    album?.let { put("album", it) }
                    trackNumber?.let { put("trackNumber", it.toString()) }
                    duration?.let { put("duration", it.toString()) }
                }
            )
            parameter("format", "json")
        }
    }

    suspend fun scrobble(
        artist: String, track: String, timestamp: Long,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null
    ) = runCatching {
        client.post {
            lastfmParams(
                method = "track.scrobble",
                apiKey = API_KEY,
                secret = SECRET,
                sessionKey = sessionKey!!,
                extra = buildMap {
                    put("artist[0]", artist)
                    put("track[0]", track)
                    put("timestamp[0]", timestamp.toString())
                    album?.let { put("album[0]", it) }
                    trackNumber?.let { put("trackNumber[0]", it.toString()) }
                    duration?.let { put("duration[0]", it.toString()) }
                }
            )
            parameter("format", "json")
        }
    }

    // API keys passed from the app module (loaded from BuildConfig/GitHub Secrets)
    private var API_KEY = ""
    private var SECRET = ""

    /**
     * Initialize LastFM with API credentials
     * @param apiKey LastFM API key
     * @param secret LastFM secret key
     */
    fun initialize(apiKey: String, secret: String) {
        API_KEY = apiKey
        SECRET = secret
    }

    const val DEFAULT_SCROBBLE_DELAY_PERCENT = 0.5f
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30
    const val DEFAULT_SCROBBLE_DELAY_SECONDS = 180
}
