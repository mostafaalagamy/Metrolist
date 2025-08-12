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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MusicXMatch {
    private val client by lazy {
        HttpClient(CIO) {
            expectSuccess = false // Handle errors manually
            
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
            }
        }
    }

    private val baseUrl = "https://www.musixmatch.com/ws/1.1/"
    private var cachedSecret: String? = null

    private suspend fun getLatestApp(): String {
        val response = client.get("https://www.musixmatch.com/search") {
            header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            header("Cookie", "mxm_bab=AB")
        }
        val htmlContent = response.bodyAsText()
        
        // Regular expression to match `_app` script URLs
        val pattern = Regex("""src="([^"]*/_next/static/chunks/pages/_app-[^"]+\.js)"""")
        val matches = pattern.findAll(htmlContent).map { it.groupValues[1] }.toList()
        
        if (matches.isEmpty()) {
            throw Exception("_app URL not found in the HTML content.")
        }
        
        return matches.last() // Get the last match if multiple are found
    }

    private suspend fun getSecret(): String {
        if (cachedSecret != null) {
            return cachedSecret!!
        }
        
        val appUrl = getLatestApp()
        val response = client.get(appUrl) {
            header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }
        val javascriptCode = response.bodyAsText()
        
        // Regular expression to capture the string inside `from(...)`
        val pattern = Regex("""from\(\s*"(.*?)"\s*\.split""")
        val match = pattern.find(javascriptCode)
        
        if (match != null) {
            val encodedString = match.groupValues[1]
            val reversedString = encodedString.reversed()
            
            // Decode the reversed string from Base64
            val decodedBytes = Base64.getDecoder().decode(reversedString)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            
            cachedSecret = decodedString
            return decodedString
        } else {
            throw Exception("Encoded string not found in the JavaScript code.")
        }
    }

    private suspend fun generateSignature(url: String): String {
        val secret = getSecret()
        val currentDate = LocalDateTime.now()
        val l = currentDate.year.toString()
        val s = currentDate.monthValue.toString().padStart(2, '0')
        val r = currentDate.dayOfMonth.toString().padStart(2, '0')
        
        val message = (url + l + s + r).toByteArray(Charsets.UTF_8)
        val key = secret.toByteArray(Charsets.UTF_8)
        
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKeySpec)
        val hashOutput = mac.doFinal(message)
        
        val encodedSignature = Base64.getEncoder().encodeToString(hashOutput)
        val urlEncodedSignature = URLEncoder.encode(encodedSignature, "UTF-8")
        
        return "&signature=$urlEncodedSignature&signature_protocol=sha256"
    }

    private suspend fun makeRequest(url: String): String {
        try {
            // Follow Python implementation exactly
            var processedUrl = url.replace("%20", "+").replace(" ", "+")
            processedUrl = baseUrl + processedUrl
            val signedUrl = processedUrl + generateSignature(processedUrl)
            
            val response = client.get(signedUrl) {
                header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
            }
            
            val responseBody = response.bodyAsText()
            
            if (response.status.value != 200) {
                throw Exception("HTTP error: ${response.status.value}")
            }
            
            return responseBody
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun searchTracks(
        query: String,
        page: Int = 1
    ): Result<List<Track>> = runCatching {
        currentCoroutineContext().ensureActive()
        
        // Follow Python implementation exactly
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "track.search?app_id=web-desktop-app-v1.0&format=json&q=$encodedQuery&f_has_lyrics=true&page_size=100&page=$page"
        
        val responseBody = makeRequest(url)
        val json = Json { 
            isLenient = true
            ignoreUnknownKeys = true 
        }
        val musicXMatchResponse = json.decodeFromString<MusicXMatchResponse<TrackSearchBody>>(responseBody)
        
        if (musicXMatchResponse.message.header.statusCode != 200) {
            throw Exception("MusicXMatch API error: ${musicXMatchResponse.message.header.statusCode}")
        }
        
        musicXMatchResponse.message.body.trackList.map { it.track }
    }

    private suspend fun getTrackLyrics(trackId: Int): Result<String> = runCatching {
        currentCoroutineContext().ensureActive()
        
        val url = "track.lyrics.get?app_id=web-desktop-app-v1.0&format=json&track_id=$trackId"
        
        val responseBody = makeRequest(url)
        val json = Json { 
            isLenient = true
            ignoreUnknownKeys = true 
        }
        val musicXMatchResponse = json.decodeFromString<MusicXMatchResponse<LyricsBody>>(responseBody)
        
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
        
        val url = "track.richsync.get?app_id=web-desktop-app-v1.0&format=json&track_id=$trackId"
        
        val responseBody = makeRequest(url)
        val json = Json { 
            isLenient = true
            ignoreUnknownKeys = true 
        }
        val musicXMatchResponse = json.decodeFromString<MusicXMatchResponse<RichSyncBody>>(responseBody)
        
        if (musicXMatchResponse.message.header.statusCode != 200) {
            throw Exception("MusicXMatch API error: ${musicXMatchResponse.message.header.statusCode}")
        }
        
        val richSyncBody = musicXMatchResponse.message.body.richsync?.richsyncBody
            ?: throw Exception("No rich sync found for track ID: $trackId")
        
        // Convert Rich Sync JSON to LRC format
        convertRichSyncToLrc(richSyncBody)
    }

    private fun convertRichSyncToLrc(richSyncJson: String): String {
        try {
            val json = Json { 
                isLenient = true
                ignoreUnknownKeys = true 
            }
            
            // Parse the Rich Sync JSON - it's an array of lines
            val richSyncData = json.decodeFromString<List<RichSyncLine>>(richSyncJson)
            
            val lrcLines = mutableListOf<String>()
            
            for (line in richSyncData) {
                val startTimeSeconds = line.ts
                val minutes = (startTimeSeconds / 60).toInt()
                val seconds = (startTimeSeconds % 60).toInt()
                val centiseconds = ((startTimeSeconds % 1) * 100).toInt()
                
                // Format as LRC: [mm:ss.cc]text
                val timeTag = String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds)
                val text = line.x // The 'x' field contains the full line text
                
                lrcLines.add("$timeTag$text")
            }
            
            return lrcLines.joinToString("\n")
        } catch (e: Exception) {
            // Log error for debugging
            // If conversion fails, return the raw text without timing
            throw Exception("Failed to convert Rich Sync to LRC format: ${e.message}")
        }
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