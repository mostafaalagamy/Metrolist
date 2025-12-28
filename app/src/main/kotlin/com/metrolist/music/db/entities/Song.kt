/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

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

    @Relation(
        parentColumn = "id",
        entityColumn = "id"
    )
    val format: FormatEntity? = null,
) : LocalItem() {
    override val id: String
        get() = song.id
    override val title: String
        get() = song.title
    override val thumbnailUrl: String?
        get() = song.thumbnailUrl
    val romanizeLyrics: Boolean
        get() = song.romanizeLyrics
}
