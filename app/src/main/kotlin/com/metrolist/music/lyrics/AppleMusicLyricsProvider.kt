package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.mostafaalagamy.metrolist.applelyrics.AppleMusic
import com.mostafaalagamy.metrolist.applelyrics.LrcFormatter

object AppleMusicLyricsProvider : LyricsProvider {
    override val name = "AppleMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableAppleMusicKey] ?: false

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        return runCatching {
            val track = AppleMusic.searchSong(title, artist)
                ?: return@runCatching "No lyrics found"

            val lyricsResponse = AppleMusic.getLyrics(track.id)
                ?: return@runCatching "No lyrics found"

            LrcFormatter.formatLyrics(lyricsResponse)
        }
    }
}
