package com.metrolist.music.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class LyricsEntry(
    val time: Long,
    val text: String,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    val translatedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
    val isTranslating: MutableStateFlow<Boolean> = MutableStateFlow(false)
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}