package com.metrolist.music.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<Word>? = null,
    val voice: String? = null,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null)
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}

data class Word(
    val text: String,
    val startTime: Long,
    val endTime: Long
)
