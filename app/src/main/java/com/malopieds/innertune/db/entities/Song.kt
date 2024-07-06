package com.malopieds.innertune.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Song
    @JvmOverloads
    constructor(
        @Embedded val song: SongEntity,
        @Relation(
            entity = ArtistEntity::class,
            entityColumn = "id",
            parentColumn = "id",
            associateBy =
                Junction(
                    value = SortedSongArtistMap::class,
                    parentColumn = "songId",
                    entityColumn = "artistId",
                ),
        )
        val artists: List<ArtistEntity>,
        @Relation(
            entity = AlbumEntity::class,
            entityColumn = "id",
            parentColumn = "id",
            associateBy =
                Junction(
                    value = SongAlbumMap::class,
                    parentColumn = "songId",
                    entityColumn = "albumId",
                ),
        )
        val album: AlbumEntity? = null,
    ) : LocalItem() {
        override val id: String
            get() = song.id
    }
