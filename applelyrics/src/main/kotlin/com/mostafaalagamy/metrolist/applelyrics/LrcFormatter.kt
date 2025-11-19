package com.mostafaalagamy.metrolist.applelyrics

import com.mostafaalagamy.metrolist.applelyrics.models.Line
import com.mostafaalagamy.metrolist.applelyrics.models.LyricsResponse

object LrcFormatter {

    fun formatLyrics(lyricsResponse: LyricsResponse): String {
        return buildString {
            lyricsResponse.content.forEach { line ->
                appendLine(formatLine(line))
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
