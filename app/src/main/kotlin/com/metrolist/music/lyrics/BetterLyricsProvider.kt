package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.mostafaalagamy.metrolist.betterlyrics.BetterLyrics

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: false

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.fetchLyrics(title, artist, duration, callback)
    }
}
