package com.metrolist.music.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class Word(
    val text: String,
    val startTime: Long,
    val endTime: Long
)

data class LyricsEntry(
    val time: Long,
    val text: String,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    val words: List<Word>? = null,
    val voice: String? = null
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}