package com.malopieds.innertune.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
            Junction(
                value = PlaylistSongMapPreview::class,
                parentColumn = "playlistId",
                entityColumn = "songId",
            ),
    )
    val thumbnails: List<String>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
}
