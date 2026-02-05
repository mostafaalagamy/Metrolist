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
@Entity(tableName = "blocked_artist")
data class BlockedArtist(
    @PrimaryKey val artistId: String,
    val artistName: String,
    val thumbnailUrl: String? = null,
    val blockedAt: LocalDateTime = LocalDateTime.now()
)
