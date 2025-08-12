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
        val titleSimilarity = calculateSimilarity(
            normalizeForMatching(title), 
            normalizeForMatching(track.trackName)
        )
        val artistSimilarity = calculateSimilarity(
            normalizeForMatching(artist), 
            normalizeForMatching(track.artistName)
        )
        
        var score = (titleSimilarity + artistSimilarity) / 2.0
        
        // Add bonus for duration match (if provided)
        if (duration > 0 && track.trackLength != null) {
            val durationDiff = abs(track.trackLength - duration)
            // More lenient duration matching - allow up to 30 seconds difference
            val durationSimilarity = when {
                durationDiff <= 5 -> 1.0 // Perfect match
                durationDiff <= 15 -> 0.9 // Very close
                durationDiff <= 30 -> 0.7 // Close enough
                else -> maxOf(0.0, 1.0 - (durationDiff / 600.0)) // Gradual decrease
            }
            score = (score * 0.7) + (durationSimilarity * 0.3)
        }
        
        // Boost tracks that have lyrics
        if (track.hasLyrics == 1) {
            score += 0.15
        }
        
        // Boost tracks that have rich sync
        if (track.hasRichsync == 1) {
            score += 0.1
        }
        
        // Extra boost for very high title similarity (likely exact match)
        if (titleSimilarity > 0.9) {
            score += 0.2
        }
        
        // Extra boost for very high artist similarity
        if (artistSimilarity > 0.9) {
            score += 0.1
        }
        
        score
    }?.takeIf { track ->
        // Only return if we have reasonable similarity (lowered threshold)
        val titleSim = calculateSimilarity(normalizeForMatching(title), normalizeForMatching(track.trackName))
        val artistSim = calculateSimilarity(normalizeForMatching(artist), normalizeForMatching(track.artistName))
        
        // More lenient matching - accept if either title OR artist has good similarity
        titleSim > 0.4 || artistSim > 0.4 || (titleSim + artistSim) / 2.0 > 0.35
    }
}

private fun normalizeForMatching(text: String): String {
    return text.lowercase()
        // Remove common words/phrases that might interfere
        .replace("\\b(the|a|an|and|or|of|in|on|at|to|for|with|by)\\b".toRegex(), " ")
        .replace("\\b(feat\\.?|ft\\.?|featuring)\\b.*".toRegex(), "")
        .replace("\\b(official|video|lyric|lyrics|audio|song)\\b".toRegex(), "")
        .replace("\\b(remix|acoustic|live|version|remaster|remastered)\\b".toRegex(), "")
        
        // Clean punctuation but preserve Arabic/English characters
        .replace("[^\\p{L}\\p{N}\\s]".toRegex(), " ")
        
        // Normalize whitespace
        .replace("\\s+".toRegex(), " ")
        .trim()
}

private fun calculateSimilarity(s1: String, s2: String): Double {
    val maxLength = maxOf(s1.length, s2.length)
    if (maxLength == 0) return 1.0
    
    // Use multiple similarity metrics and take the best one
    val levenshteinSim = 1.0 - (levenshteinDistance(s1, s2).toDouble() / maxLength)
    val jaccardSim = jaccardSimilarity(s1, s2)
    val containsSim = containsSimilarity(s1, s2)
    
    // Return the highest similarity score
    return maxOf(levenshteinSim, jaccardSim, containsSim)
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

private fun jaccardSimilarity(s1: String, s2: String): Double {
    val words1 = s1.split("\\s+".toRegex()).filter { it.isNotEmpty() }.toSet()
    val words2 = s2.split("\\s+".toRegex()).filter { it.isNotEmpty() }.toSet()
    
    if (words1.isEmpty() && words2.isEmpty()) return 1.0
    if (words1.isEmpty() || words2.isEmpty()) return 0.0
    
    val intersection = words1.intersect(words2).size
    val union = words1.union(words2).size
    
    return intersection.toDouble() / union
}

private fun containsSimilarity(s1: String, s2: String): Double {
    val shorter = if (s1.length < s2.length) s1 else s2
    val longer = if (s1.length >= s2.length) s1 else s2
    
    return if (longer.contains(shorter, ignoreCase = true)) {
        shorter.length.toDouble() / longer.length
    } else {
        // Check if most words from shorter string exist in longer string
        val shorterWords = shorter.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val longerWords = longer.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        if (shorterWords.isEmpty()) return 0.0
        
        val matchingWords = shorterWords.count { word ->
            longerWords.any { it.contains(word, ignoreCase = true) || word.contains(it, ignoreCase = true) }
        }
        
        matchingWords.toDouble() / shorterWords.size * 0.7 // Slightly lower score for partial matches
    }
}