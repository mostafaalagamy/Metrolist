package com.zionhuang.music.lyrics

import android.content.Context
import com.zionhuang.lrclib.LrcLib

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"
    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(id: String, title: String, artist: String, duration: Int, callback: (String) -> Unit) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
