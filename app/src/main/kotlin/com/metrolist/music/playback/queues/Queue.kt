/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) {
                copy(
                    items = items.filterExplicit(),
                )
            } else {
                this
            }

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) {
                copy(
                    items = items.filterVideoSongs(true),
                )
            } else {
                this
            }

        fun filterBlocked(
            blockedSongs: Set<String>,
            blockedArtists: Set<String>,
            blockedAlbums: Set<String>
        ) = copy(items = items.filterBlocked(blockedSongs, blockedArtists, blockedAlbums))
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }

fun List<MediaItem>.filterBlocked(
    blockedSongs: Set<String>,
    blockedArtists: Set<String>,
    blockedAlbums: Set<String>
) = filterNot { item ->
    val meta = item.metadata ?: return@filterNot false
    if (meta.id in blockedSongs) return@filterNot true
    if (meta.album?.id in blockedAlbums) return@filterNot true
    
    // Check primary artist
    // MediaMetadata.artist is Artist object { id, name }
    meta.artists.firstOrNull()?.id?.let { if (it in blockedArtists) return@filterNot true }
    
    false
}
