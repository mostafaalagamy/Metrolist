package com.metrolist.musicxmatch.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class MusicXMatchResponse<T>(
    val message: MusicXMatchMessage<T>
)

@Serializable
data class MusicXMatchMessage<T>(
    val header: MusicXMatchHeader,
    val body: T
)

@Serializable
data class MusicXMatchHeader(
    @SerialName("status_code")
    val statusCode: Int,
    @SerialName("execute_time")
    val executeTime: Double? = null,
    val available: Int? = null,
    val hint: String? = null
)

// Search Response Models
@Serializable
data class TrackSearchBody(
    @SerialName("track_list")
    val trackList: List<TrackWrapper>
)

@Serializable
data class TrackWrapper(
    val track: Track
)

@Serializable
data class Track(
    @SerialName("track_id")
    val trackId: Int,
    @SerialName("track_name")
    val trackName: String,
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("album_name")
    val albumName: String? = null,
    @SerialName("track_length")
    val trackLength: Int? = null,
    @SerialName("commontrack_id")
    val commontrackId: Int? = null,
    @SerialName("has_lyrics")
    val hasLyrics: Int? = null,
    @SerialName("has_richsync")
    val hasRichsync: Int? = null
)

// Lyrics Response Models
@Serializable
data class LyricsBody(
    val lyrics: Lyrics? = null
)

@Serializable
data class Lyrics(
    @SerialName("lyrics_id")
    val lyricsId: Int,
    @SerialName("lyrics_body")
    val lyricsBody: String,
    @SerialName("lyrics_language")
    val lyricsLanguage: String? = null,
    @SerialName("lyrics_language_description")
    val lyricsLanguageDescription: String? = null,
    @SerialName("script_tracking_url")
    val scriptTrackingUrl: String? = null,
    @SerialName("pixel_tracking_url")
    val pixelTrackingUrl: String? = null,
    @SerialName("lyrics_copyright")
    val lyricsCopyright: String? = null,
    @SerialName("updated_time")
    val updatedTime: String? = null
)

// RichSync Response Models
@Serializable
data class RichSyncBody(
    val richsync: RichSync? = null
)

@Serializable
data class RichSync(
    @SerialName("richsync_id")
    val richsyncId: Int,
    @SerialName("richsync_body")
    val richsyncBody: String,
    @SerialName("richsync_language")
    val richsyncLanguage: String? = null,
    @SerialName("richsync_language_description")
    val richsyncLanguageDescription: String? = null,
    @SerialName("richsync_length")
    val richsyncLength: Int? = null,
    @SerialName("lyrics_copyright")
    val lyricsCopyright: String? = null,
    @SerialName("updated_time")
    val updatedTime: String? = null
)

// Rich Sync JSON format models
@Serializable
data class RichSyncLine(
    val ts: Double, // Start time in seconds
    val te: Double, // End time in seconds  
    val x: String   // Full line text
)

// Helper function for finding best match
internal fun List<Track>.bestMatchingFor(
    title: String,
    artist: String,
    duration: Int = -1
): Track? {
    if (isEmpty()) return null
    
    return maxByOrNull { track ->
        val titleSimilarity = calculateSimilarity(title.lowercase(), track.trackName.lowercase())
        val artistSimilarity = calculateSimilarity(artist.lowercase(), track.artistName.lowercase())
        
        var score = (titleSimilarity + artistSimilarity) / 2.0
        
        // Add bonus for duration match (if provided)
        if (duration > 0 && track.trackLength != null) {
            val durationDiff = abs(track.trackLength - duration)
            val durationSimilarity = if (durationDiff <= 10) 1.0 else maxOf(0.0, 1.0 - (durationDiff / 300.0))
            score = (score * 0.8) + (durationSimilarity * 0.2)
        }
        
        // Boost tracks that have lyrics
        if (track.hasLyrics == 1) {
            score += 0.1
        }
        
        // Boost tracks that have rich sync
        if (track.hasRichsync == 1) {
            score += 0.05
        }
        
        score
    }
}

private fun calculateSimilarity(s1: String, s2: String): Double {
    val maxLength = maxOf(s1.length, s2.length)
    if (maxLength == 0) return 1.0
    
    val distance = levenshteinDistance(s1, s2)
    return 1.0 - (distance.toDouble() / maxLength)
}

private fun levenshteinDistance(s1: String, s2: String): Int {
    val m = s1.length
    val n = s2.length
    val dp = Array(m + 1) { IntArray(n + 1) }

    for (i in 0..m) dp[i][0] = i
    for (j in 0..n) dp[0][j] = j

    for (i in 1..m) {
        for (j in 1..n) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,     // deletion
                dp[i][j - 1] + 1,     // insertion
                dp[i - 1][j - 1] + cost // substitution
            )
        }
    }
    return dp[m][n]
}