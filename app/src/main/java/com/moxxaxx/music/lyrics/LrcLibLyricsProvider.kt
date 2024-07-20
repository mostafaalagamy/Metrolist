package com.moxxaxx.music.lyrics

import android.content.Context
import com.moxxaxx.music.constants.EnableLrcLibKey
import com.moxxaxx.music.utils.dataStore
import com.moxxaxx.music.utils.get
import com.moxxaxx.lrclib.LrcLib

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
