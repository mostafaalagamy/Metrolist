/*
 * Copyright (C) 2026 Metrolist Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class EventWithSong(
    @Embedded
    val event: Event,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "songId",
        entityColumn = "id",
    )
    val song: Song,
)
