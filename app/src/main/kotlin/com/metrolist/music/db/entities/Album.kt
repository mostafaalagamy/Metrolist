package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Ignore
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
    val artists: List<ArtistEntity> = emptyList(),
) : LocalItem() {
    @Ignore var songCountListened: Int = 0
    @Ignore var timeListened: Long = 0
    override val id: String
        get() = album.id
    override val title: String
        get() = album.title
    override val thumbnailUrl: String?
        get() = album.thumbnailUrl
}
