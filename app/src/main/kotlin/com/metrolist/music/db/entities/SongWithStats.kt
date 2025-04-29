package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class SongWithStats(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val songCountListened: Int,
    val timeListened: Long?,
)
