package com.metrolist.music.utils

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.MusicDatabase
import timber.log.Timber

/**
 * Extension functions to filter YouTube API content by artist whitelist.
 *
 * These functions check if content should be displayed based on whether
 * the associated artists are in the artist_whitelist table.
 */

/**
 * Check if a song should be displayed based on whitelist.
 * Returns true if ANY of the song's artists are whitelisted.
 * If whitelist is empty, returns false (show nothing).
 */
suspend fun SongItem.isWhitelisted(database: MusicDatabase): Boolean {
    // If no artists, can't determine whitelist status - default to not whitelisted
    if (this.artists.isEmpty()) return false

    // Check if ANY artist is whitelisted
    return this.artists.any { artist ->
        val artistId = artist.id ?: return@any false
        database.isArtistWhitelisted(artistId)
    }
}

/**
 * Check if an album should be displayed based on whitelist.
 * Returns true if ANY of the album's artists are whitelisted.
 * If whitelist is empty, returns false (show nothing).
 */
suspend fun AlbumItem.isWhitelisted(database: MusicDatabase): Boolean {
    // If no artists, can't determine whitelist status - default to not whitelisted
    val albumArtists = this.artists ?: return false
    if (albumArtists.isEmpty()) return false

    // Check if ANY artist is whitelisted
    return albumArtists.any { artist ->
        val artistId = artist.id ?: return@any false
        database.isArtistWhitelisted(artistId)
    }
}

/**
 * Check if an artist should be displayed based on whitelist.
 * Returns true if the artist is whitelisted.
 * If whitelist is empty, returns false (show nothing).
 */
suspend fun ArtistItem.isWhitelisted(database: MusicDatabase): Boolean {
    return database.isArtistWhitelisted(this.id)
}

/**
 * Check if a playlist should be displayed based on whitelist.
 * Returns true if the playlist author/curator is whitelisted.
 */
suspend fun PlaylistItem.isWhitelisted(database: MusicDatabase): Boolean {
    // Check if playlist author is whitelisted
    val authorId = this.author?.id ?: return false
    return database.isArtistWhitelisted(authorId)
}

/**
 * Filter a list of YTItems by whitelist.
 * Only items whose artists are whitelisted will be returned.
 */
suspend fun List<YTItem>.filterWhitelisted(database: MusicDatabase): List<YTItem> {
    Timber.d("WhitelistFilter: Filtering ${this.size} items")
    val result = this.filter { item ->
        val isWhitelisted = when (item) {
            is SongItem -> item.isWhitelisted(database).also {
                Timber.d("WhitelistFilter: SongItem '${item.title}' by ${item.artists.joinToString { it.name }} - whitelisted=$it")
            }
            is AlbumItem -> item.isWhitelisted(database).also {
                Timber.d("WhitelistFilter: AlbumItem '${item.title}' by ${item.artists?.joinToString { it.name }} - whitelisted=$it")
            }
            is ArtistItem -> item.isWhitelisted(database).also {
                Timber.d("WhitelistFilter: ArtistItem '${item.title}' (${item.id}) - whitelisted=$it")
            }
            is PlaylistItem -> item.isWhitelisted(database).also {
                Timber.d("WhitelistFilter: PlaylistItem '${item.title}' - whitelisted=$it")
            }
            else -> false.also {
                Timber.d("WhitelistFilter: Unknown item type ${item.javaClass.simpleName} - filtered out")
            }
        }
        isWhitelisted
    }
    Timber.d("WhitelistFilter: Result: ${result.size} items passed filter (${this.size - result.size} filtered out)")
    return result
}
