package com.metrolist.music.db.entities

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "playlist_song_map_preview",
    value = "SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position",
)
data class PlaylistSongMapPreview(
    @ColumnInfo(index = true) val playlistId: String,
    @ColumnInfo(index = true) val songId: String,
    val idInPlaylist: Int = 0,
)
