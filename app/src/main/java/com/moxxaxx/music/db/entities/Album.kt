package com.moxxaxx.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Album(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
            Junction(
                value = AlbumArtistMap::class,
                parentColumn = "albumId",
                entityColumn = "artistId",
            ),
    )
    val artists: List<ArtistEntity>,
) : LocalItem() {
    override val id: String
        get() = album.id
}
