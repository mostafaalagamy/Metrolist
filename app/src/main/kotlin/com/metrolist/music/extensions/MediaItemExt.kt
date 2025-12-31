/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata

/**
 * Extension property to safely extract MediaMetadata from MediaItem
 * Returns null if metadata is not available or invalid
 */
val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

/**
 * Converts a Song entity to MediaItem with full metadata
 * Enhanced with better null safety and error handling
 */
fun Song.toMediaItem(): MediaItem {
    val artistNames = artists.joinToString(", ") { it.name }
    
    return MediaItem.Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCustomCacheKey(song.id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(song.title)
                .setSubtitle(artistNames)
                .setArtist(artistNames)
                .apply {
                    // Use thumbnail without forcing specific resolution
                    song.thumbnailUrl?.let { url ->
                        setArtworkUri(url.toUri())
                    }
                }
                .setAlbumTitle(song.albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .apply {
                    // Add duration if available
                    if (song.duration > 0) {
                        setDurationMs(song.duration)
                    }
                }
                .build()
        )
        .build()
}

/**
 * Converts a SongItem from YouTube Music API to MediaItem
 * Updated to support latest API response structure
 */
fun SongItem.toMediaItem(): MediaItem {
    val artistNames = artists.joinToString(", ") { it.name }
    
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(artistNames)
                .setArtist(artistNames)
                .apply {
                    // Use thumbnail URL as-is from API
                    setArtworkUri(thumbnail.toUri())
                }
                .setAlbumTitle(album?.name)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .apply {
                    // Add duration if available from API
                    duration?.let { durationSeconds ->
                        setDurationMs((durationSeconds * 1000).toLong())
                    }
                    
                    // Add explicit content flag if available
                    if (explicit == true) {
                        setIsExplicit(true)
                    }
                }
                .build()
        )
        .build()
}

/**
 * Converts MediaMetadata model to MediaItem
 * Enhanced with additional metadata fields
 */
fun MediaMetadata.toMediaItem(): MediaItem {
    val artistNames = artists.joinToString(", ") { it.name }
    
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(artistNames)
                .setArtist(artistNames)
                .apply {
                    // Use thumbnail URL without modification
                    thumbnailUrl?.let { url ->
                        setArtworkUri(url.toUri())
                    }
                }
                .setAlbumTitle(album?.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .apply {
                    // Add duration
                    duration?.let { durationMs ->
                        setDurationMs(durationMs)
                    }
                    
                    // Add album artist if available
                    album?.id?.let {
                        setAlbumArtist(artists.firstOrNull()?.name)
                    }
                }
                .build()
        )
        .build()
}

/**
 * Extension function to create a list of MediaItems from Songs
 * Optimized for batch operations
 */
fun List<Song>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

/**
 * Extension function to create a list of MediaItems from SongItems
 * Optimized for batch operations with YouTube Music API
 */
fun List<SongItem>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

/**
 * Helper function to validate MediaItem has required metadata
 */
fun MediaItem.hasValidMetadata(): Boolean {
    return mediaMetadata.title != null && 
           !mediaMetadata.title.toString().isBlank() &&
           localConfiguration?.uri != null
}

/**
 * Helper function to get safe media ID
 */
val MediaItem.safeMediaId: String
    get() = mediaId.takeIf { !it.isNullOrBlank() } ?: localConfiguration?.uri?.toString() ?: ""
