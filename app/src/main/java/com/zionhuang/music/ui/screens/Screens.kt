package com.zionhuang.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.zionhuang.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconId: Int,
    val route: String,
) {
    object Home : Screens(R.string.home, R.drawable.home, "home")
    object Songs : Screens(R.string.songs, R.drawable.music_note, "songs")
    object Artists : Screens(R.string.artists, R.drawable.artist, "artists")
    object Albums : Screens(R.string.albums, R.drawable.album, "albums")
    object Playlists : Screens(R.string.playlists, R.drawable.queue_music, "playlists")

    companion object {
        val MainScreens = listOf(Home, Songs, Artists, Albums, Playlists)
    }
}
