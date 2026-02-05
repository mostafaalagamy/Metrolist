/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "blocked_song")
data class BlockedSong(
    @PrimaryKey val songId: String,
    val songTitle: String,
    val artistName: String? = null,
    val thumbnailUrl: String? = null,
    val blockedAt: LocalDateTime = LocalDateTime.now()
)
