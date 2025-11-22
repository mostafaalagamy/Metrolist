package com.metrolist.music.lyrics

import androidx.compose.runtime.mutableStateOf
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.metrolist.music.db.entities.LyricsEntity
import kotlinx.coroutines.flow.MutableStateFlow

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<Word>? = null,
    val voice: String? = null,
) : Comparable<LyricsEntry> {
    val romanizedTextFlow = MutableStateFlow<String?>(null)
    override fun compareTo(other: LyricsEntry): Int {
        return time.compareTo(other.time)
    }

    companion object {
        val HEAD_LYRICS_ENTRY =
            LyricsEntry(
                time = 0,
                text = "",
            )
    }
}

data class Word(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val syllables: List<Syllable>
)

data class Syllable(
    val text: String,
    val startTime: Long,
    val endTime: Long
)