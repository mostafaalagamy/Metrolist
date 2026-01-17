/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.utils.resize

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

fun Song.toMediaItem() = MediaItem.Builder()
    .setMediaId(song.id)
    .setUri(song.id)
    .setCustomCacheKey(song.id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song.title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(song.thumbnailUrl?.toUri())
            .setAlbumTitle(song.albumName)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(song.title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putString("artwork_uri", song.thumbnailUrl)
            })
            .build()
    )
    .build()

fun SongItem.toMediaItem() = MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(toMediaMetadata())
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnail.resize(544, 544).toUri())
            .setAlbumTitle(album?.name)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                putString("artwork_uri", thumbnail.resize(544, 544))
            })
            .build()
    )
    .build()

fun MediaMetadata.toMediaItem() = MediaItem.Builder()
    .setMediaId(id)
    .setUri(id)
    .setCustomCacheKey(id)
    .setTag(this)
    .setMediaMetadata(
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(artists.joinToString { it.name })
            .setArtist(artists.joinToString { it.name })
            .setArtworkUri(thumbnailUrl?.toUri())
            .setAlbumTitle(album?.title)
            .setAlbumArtist(artists.firstOrNull()?.name)
            .setDisplayTitle(title)
            .setMediaType(MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply {
                thumbnailUrl?.let { putString("artwork_uri", it) }
            })
            .build()
    )
    .build()
