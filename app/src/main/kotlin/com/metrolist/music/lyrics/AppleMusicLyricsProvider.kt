package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.constants.EnableAppleMusicKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.mostafaalagamy.metrolist.applelyrics.AppleMusic
import com.mostafaalagamy.metrolist.applelyrics.LrcFormatter
import timber.log.Timber

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
            Timber.d("AppleMusic: Searching for '$title' by '$artist'")
            val track = AppleMusic.searchSong(title, artist)
            if (track == null) {
                Timber.w("AppleMusic: No track found for '$title' by '$artist'")
                return@runCatching "No lyrics found"
            }
            Timber.d("AppleMusic: Found track with ID ${track.id}")

            val rawLyrics = AppleMusic.getLyrics(track.id)
            if (rawLyrics == null) {
                Timber.w("AppleMusic: No lyrics found for track ID ${track.id}")
                return@runCatching "No lyrics found"
            }
            Timber.d("AppleMusic: Successfully fetched raw lyrics for track ID ${track.id}:\n$rawLyrics")

            LrcFormatter.formatLyrics(rawLyrics) ?: "No lyrics found"
        }.onFailure {
            Timber.e(it, "AppleMusic: Error fetching lyrics for '$title' by '$artist'")
        }
    }
}
