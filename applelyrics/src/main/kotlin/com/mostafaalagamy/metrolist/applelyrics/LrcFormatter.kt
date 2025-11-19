package com.mostafaalagamy.metrolist.applelyrics

import com.mostafaalagamy.metrolist.applelyrics.models.Line
import com.mostafaalagamy.metrolist.applelyrics.models.LyricsResponse
import kotlinx.serialization.json.Json

object LrcFormatter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun formatLyrics(rawLyrics: String): String? {
        return try {
            // First, try to parse as a LyricsResponse object
            val lyricsResponse = json.decodeFromString<LyricsResponse>(rawLyrics)
            buildString {
                lyricsResponse.content.forEach { line ->
                    appendLine(formatLine(line))
                }
            }
        } catch (e: Exception) {
            // If that fails, try to parse as a direct list of lines
            try {
                val lines = json.decodeFromString<List<Line>>(rawLyrics)
                buildString {
                    lines.forEach { line ->
                        appendLine(formatLine(line))
                    }
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun formatLine(line: Line): String {
        val lineTimestamp = toLrcTimestamp(line.timestamp)
        val voice = if (line.oppositeTurn == true) "v2:" else "v1:"
        val lineText = buildString {
            line.text.forEach { syllable ->
                append("<${toLrcTimestamp(syllable.timestamp)}>")
                append(syllable.text)
                if (syllable.endtime != null) {
                    append("<${toLrcTimestamp(syllable.endtime)}>")
                }
                if (syllable.part != true) {
                    append(" ")
                }
            }
        }
        return "[$lineTimestamp]$voice$lineText"
    }

    private fun toLrcTimestamp(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (milliseconds % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
}
