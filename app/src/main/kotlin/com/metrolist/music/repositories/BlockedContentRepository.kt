package com.metrolist.music.repositories

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.BlockedAlbum
import com.metrolist.music.db.entities.BlockedArtist
import com.metrolist.music.db.entities.BlockedSong
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.AlbumItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedContentRepository @Inject constructor(
    private val database: MusicDatabase
) {
    val blockedSongs = database.blockedSongs()
    val blockedArtists = database.blockedArtists()
    val blockedAlbums = database.blockedAlbums()

    suspend fun blockSong(songId: String, title: String, artist: String?, thumb: String?) {
        database.insert(BlockedSong(songId, title, artist, thumb))
    }

    suspend fun blockArtist(artistId: String, name: String, thumb: String?) {
        database.insert(BlockedArtist(artistId, name, thumb))
    }

    suspend fun blockAlbum(albumId: String, title: String, artist: String?, thumb: String?) {
        database.insert(BlockedAlbum(albumId, title, artist, thumb))
    }

    suspend fun unblockSong(songId: String) {
        database.deleteBlockedSong(songId)
    }

    suspend fun unblockArtist(artistId: String) {
        database.deleteBlockedArtist(artistId)
    }

    suspend fun unblockAlbum(albumId: String) {
        database.deleteBlockedAlbum(albumId)
    }

    // Helper to check if a song should be filtered out
    suspend fun shouldBlock(song: Song): Boolean {
        // Check song ID
        if (database.isSongBlocked(song.id).first()) return true
        
        // Check album ID
        song.album?.id?.let { albumId ->
            if (database.isAlbumBlocked(albumId).first()) return true
        }

        // Check artist IDs (primary and others)
        // User requested: "Block only block when they're the main artist, allow features"
        // But for "Block all songs by artist", usually this means if the main artist is blocked.
        // The `artists` list usually has the main artist first (position 0).
        // Let's block if the *primary* artist is blocked.
        val primaryArtist = song.artists.firstOrNull()
        if (primaryArtist != null) {
            if (database.isArtistBlocked(primaryArtist.id).first()) return true
        }

        return false
    }

    // Helper to filter a list of songs
    suspend fun filterBlocked(songs: List<Song>): List<Song> {
        val blockedSongIds = database.blockedSongs().first().map { it.songId }.toSet()
        val blockedArtistIds = database.blockedArtists().first().map { it.artistId }.toSet()
        val blockedAlbumIds = database.blockedAlbums().first().map { it.albumId }.toSet()

        return songs.filterNot { song ->
            if (song.id in blockedSongIds) return@filterNot true
            if (song.album?.id in blockedAlbumIds) return@filterNot true
            
            // Check primary artist
            val primaryArtistId = song.artists.firstOrNull()?.id
            if (primaryArtistId != null && primaryArtistId in blockedArtistIds) return@filterNot true
            
            false
        }
    }

    suspend fun filterBlockedAlbums(albums: List<Album>): List<Album> {
        val blockedAlbumIds = database.blockedAlbums().first().map { it.albumId }.toSet()
        // Optionally filter if artist is blocked? For now just explicit album blocks.
        return albums.filterNot { it.id in blockedAlbumIds }
    }

    suspend fun filterBlockedArtists(artists: List<Artist>): List<Artist> {
        val blockedArtistIds = database.blockedArtists().first().map { it.artistId }.toSet()
        return artists.filterNot { it.id in blockedArtistIds }
    }

    suspend fun filterBlockedYTItems(items: List<YTItem>): List<YTItem> {
        val blockedSongIds = database.blockedSongs().first().map { it.songId }.toSet()
        val blockedArtistIds = database.blockedArtists().first().map { it.artistId }.toSet()
        val blockedAlbumIds = database.blockedAlbums().first().map { it.albumId }.toSet()

        return items.filterNot { item ->
            when (item) {
                is SongItem -> {
                    if (item.id in blockedSongIds) return@filterNot true
                    if (item.album?.id in blockedAlbumIds) return@filterNot true
                    // SongItem artists are List<Artist> { name, id? }
                    // Check primary artist
                    item.artists.firstOrNull()?.id?.let { if (it in blockedArtistIds) return@filterNot true }
                    false
                }
                is ArtistItem -> item.id in blockedArtistIds
                is AlbumItem -> {
                    if (item.id in blockedAlbumIds) return@filterNot true
                    item.artists?.firstOrNull()?.id?.let { if (it in blockedArtistIds) return@filterNot true }
                    false
                }
                else -> false
            }
        }
    }
}
