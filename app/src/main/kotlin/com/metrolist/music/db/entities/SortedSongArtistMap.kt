package com.metrolist.music.db.entities

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "sorted_song_artist_map",
    value = "SELECT * FROM song_artist_map ORDER BY position",
)
data class SortedSongArtistMap(
    @ColumnInfo(index = true) val songId: String,
    @ColumnInfo(index = true) val artistId: String,
    val position: Int,
)
