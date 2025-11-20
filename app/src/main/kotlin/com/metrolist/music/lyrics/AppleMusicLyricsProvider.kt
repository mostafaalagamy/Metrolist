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
                ?: throw Exception("No track found for '$title' by '$artist'")
            Timber.d("AppleMusic: Found track with ID ${track.id}")

            val rawLyrics = AppleMusic.getLyrics(track.id)
                ?: throw Exception("No lyrics found for track ID ${track.id}")
            Timber.d("AppleMusic: Successfully fetched raw lyrics for track ID ${track.id}:\n$rawLyrics")

            LrcFormatter.formatLyrics(rawLyrics)
                ?: throw Exception("Failed to format lyrics for track ID ${track.id}")
        }.onFailure {
            Timber.e(it, "AppleMusic: Error fetching lyrics for '$title' by '$artist'")
        }
    }
}
