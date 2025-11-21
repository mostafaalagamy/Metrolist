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
            val tracks = AppleMusic.searchSong(title, artist)
                ?: throw Exception("No tracks found for '$title' by '$artist'")
            Timber.d("AppleMusic: Found ${tracks.size} tracks for '$title' by '$artist'")

            val bestMatch = tracks.maxByOrNull { track ->
                var score = 0
                if (track.songName.equals(title, ignoreCase = true)) score += 10
                if (track.artistName.equals(artist, ignoreCase = true)) score += 5
                if (title.contains(track.songName, ignoreCase = true)) score += 2
                if (artist.contains(track.artistName, ignoreCase = true)) score += 1
                score
            } ?: throw Exception("No suitable match found after scoring")

            Timber.d("AppleMusic: Best match is '${bestMatch.songName}' by '${bestMatch.artistName}'")

            val rawLyrics = AppleMusic.getLyrics(bestMatch.id)
                ?: throw Exception("No lyrics found for track ID ${bestMatch.id}")
            Timber.d("AppleMusic: Successfully fetched raw lyrics for track ID ${bestMatch.id}")

            LrcFormatter.formatLyrics(rawLyrics)
                ?: throw Exception("Failed to format lyrics for track ID ${bestMatch.id}")
        }.onFailure {
            Timber.e(it, "AppleMusic: Error fetching lyrics for '$title' by '$artist'")
        }
    }
}
