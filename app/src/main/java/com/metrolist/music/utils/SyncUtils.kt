package com.metrolist.music.utils

import android.util.Log
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.innertube.utils.completedLibraryPage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    val database: MusicDatabase,
) {
    private val TAG = "SyncUtils"

    fun likeSong(s: SongEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs() = coroutineScope {
        YouTube.playlist("LM").completed().onSuccess { page ->
            val remoteSongs = page.songs.reversed()
            
            val localLikedSongs = database.likedSongsByNameAsc().first()

            // Process local songs that don't exist on YouTube
            localLikedSongs.filter { localSong ->
                remoteSongs.none { it.id == localSong.id }
            }.map { song ->
                async {
                    if (song.song.liked) {
                        database.update(song.song.localToggleLike())
                    }
                }
            }.awaitAll()

            // Process songs in original order
            remoteSongs.map { remoteSong ->
                async {
                    val dbSong = database.song(remoteSong.id).firstOrNull()
                    when {
                        dbSong == null -> {
                            database.insert(
                                remoteSong.toMediaMetadata().toSongEntity()
                                    .copy(liked = true, likedDate = LocalDateTime.now())
                            )
                        }
                        !dbSong.song.liked -> {
                            database.update(dbSong.song.localToggleLike())
                        }
                    }
                }
            }.awaitAll()
        }.onFailure { error ->
            Log.e(TAG, "Failed to sync liked songs", error)
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        YouTube.library("FEmusic_liked_videos").completedLibraryPage().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>()
            val localLibrarySongs = database.songsByNameAsc().first()

            localLibrarySongs.filterNot { localSong ->
                remoteSongs.any { it.id == localSong.id }
            }.map { song ->
                async {
                    database.update(song.song.toggleLibrary())
                }
            }.awaitAll()

            remoteSongs.map { song ->
                async {
                    val dbSong = database.song(song.id).firstOrNull()
                    when (dbSong) {
                        null -> database.insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                        else -> if (dbSong.song.inLibrary == null) database.update(dbSong.song.toggleLibrary())
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun syncLikedAlbums() = coroutineScope {
        YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess { page ->
            val albums = page.items.filterIsInstance<AlbumItem>()
            val dbAlbums = database.albumsLikedByNameAsc().first()

            dbAlbums.filterNot { it.id in albums.map(AlbumItem::id) }
                .map { album ->
                    async {
                        database.update(album.album.localToggleLike())
                    }
                }.awaitAll()

            albums.map { album ->
                async {
                    val dbAlbum = database.album(album.id).firstOrNull()
                    YouTube.album(album.browseId).onSuccess { albumPage ->
                        when (dbAlbum) {
                            null -> {
                                database.insert(albumPage)
                                database.album(album.id).firstOrNull()?.let {
                                    database.update(it.album.localToggleLike())
                                }
                            }
                            else -> if (dbAlbum.album.bookmarkedAt == null) {
                                database.update(dbAlbum.album.localToggleLike())
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun syncArtistsSubscriptions() = coroutineScope {
        YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess { page ->
            val artists = page.items.filterIsInstance<ArtistItem>()
            val dbArtists = database.artistsBookmarkedByNameAsc().first()

            dbArtists.filterNot { it.id in artists.map(ArtistItem::id) }
                .map { artist ->
                    async {
                        database.update(artist.artist.localToggleLike())
                    }
                }.awaitAll()

            artists.map { artist ->
                async {
                    val dbArtist = database.artist(artist.id).firstOrNull()
                    when (dbArtist) {
                        null -> {
                            database.insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        }
                        else -> if (dbArtist.artist.bookmarkedAt == null) {
                            database.update(dbArtist.artist.localToggleLike())
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun syncSavedPlaylists() = coroutineScope {
        YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess { page ->
            val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" } // Exclude liked songs and saved episodes
                .distinctBy { it.id } // Prevent duplicates
            
            val dbPlaylists = database.playlistsByNameAsc().first()

            // Process local playlists that don't exist on YouTube
            dbPlaylists.filter { dbPlaylist ->
                dbPlaylist.playlist.browseId != null && 
                remotePlaylists.none { it.id == dbPlaylist.playlist.browseId }
            }.map { playlist ->
                async {
                    if (playlist.playlist.bookmarkedAt != null) {
                        database.update(playlist.playlist.localToggleLike())
                    }
                }
            }.awaitAll()

            // Process playlists with change detection
            remotePlaylists.map { remotePlaylist ->
                async {
                    val existingPlaylist = dbPlaylists.find { 
                        it.playlist.browseId == remotePlaylist.id 
                    }?.playlist

                    if (existingPlaylist == null) {
                        val newPlaylist = PlaylistEntity(
                            id = UUID.randomUUID().toString(),
                            name = remotePlaylist.title,
                            browseId = remotePlaylist.id,
                            isEditable = remotePlaylist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = remotePlaylist.songCountText?.toIntOrNull(),
                            playEndpointParams = remotePlaylist.playEndpoint?.params,
                            shuffleEndpointParams = remotePlaylist.shuffleEndpoint?.params,
                            radioEndpointParams = remotePlaylist.radioEndpoint?.params
                        )
                        database.insert(newPlaylist)
                        syncPlaylist(remotePlaylist.id, newPlaylist.id)
                    } else {
                        // Update data only if changed
                        if (existingPlaylist.name != remotePlaylist.title ||
                            existingPlaylist.remoteSongCount != remotePlaylist.songCountText?.toIntOrNull()) {
                            
                            database.update(
                                existingPlaylist.copy(
                                    name = remotePlaylist.title,
                                    remoteSongCount = remotePlaylist.songCountText?.toIntOrNull()
                                )
                            )
                        }
                        
                        // Sync songs only if playlist is editable
                        if (existingPlaylist.isEditable) {
                            syncPlaylist(remotePlaylist.id, existingPlaylist.id)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        YouTube.playlist(browseId).completed().onSuccess { playlistPage ->
            // Check if songs have changed before updating
            val currentSongs = database.playlistSongs(playlistId).first()
            val currentSongIds = currentSongs.map { it.song.id }
            val newSongIds = playlistPage.songs.map { it.id }

            if (currentSongIds != newSongIds) {
                launch {
                    database.clearPlaylist(playlistId)
                    
                    // Add songs in original order
                    playlistPage.songs.forEachIndexed { position, songItem ->
                        val song = songItem.toMediaMetadata()
                        
                        if (database.song(song.id).firstOrNull() == null) {
                            database.insert(song)
                        }
                        
                        database.insert(
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistId,
                                position = position,
                                setVideoId = song.setVideoId
                            )
                        )
                    }
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to sync playlist $browseId", error)
        }
    }
}
