package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "artist_whitelist")
data class ArtistWhitelistEntity(
    @PrimaryKey val artistId: String,
    val artistName: String,
    val addedAt: LocalDateTime = LocalDateTime.now(),
    val source: String = "github",
    val lastSyncedAt: LocalDateTime = LocalDateTime.now()
)
