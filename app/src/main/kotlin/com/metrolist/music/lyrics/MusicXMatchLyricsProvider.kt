package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.musicxmatch.MusicXMatch
import com.metrolist.music.constants.EnableMusicXMatchKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get

object MusicXMatchLyricsProvider : LyricsProvider {
    override val name = "MusicXMatch"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableMusicXMatchKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = MusicXMatch.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        MusicXMatch.getAllLyrics(title, artist, duration, callback)
    }
}