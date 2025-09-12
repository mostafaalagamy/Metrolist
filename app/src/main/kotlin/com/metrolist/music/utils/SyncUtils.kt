package com.metrolist.music.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val syncContext = newSingleThreadContext("syncUtils")

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs() = coroutineScope {
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remoteSongs = page.songs
                val remoteIds = remoteSongs.map { it.id }
                val localSongs = database.likedSongsByNameAsc().first()

                // Update local songs that are no longer liked remotely
                localSongs.filterNot { it.id in remoteIds }
                    .forEach { 
                        try {
                            database.transaction {
                                database.update(it.song.localToggleLike())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                val now = LocalDateTime.now()
                remoteSongs.forEachIndexed { index, song ->
                    launch(syncContext) {
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                            database.transaction {
                                if (dbSong == null) {
                                    // Use proper MediaMetadata insertion to save artist information
                                    insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                                } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                    update(dbSong.song.copy(liked = true, likedDate = timestamp))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        try {
            YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.songsByNameAsc().first()
                val feedbackTokens = mutableListOf<String>()

                localSongs.filterNot { it.id in remoteIds }
                    .forEach { 
                        if (it.song.libraryAddToken != null && it.song.libraryRemoveToken != null) {
                            feedbackTokens.add(it.song.libraryAddToken)
                        } else {
                            try {
                                database.transaction {
                                    database.update(it.song.toggleLibrary())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                feedbackTokens.chunked(20).forEach {
                    YouTube.feedback(it)
                }

                remoteSongs.forEach { song ->
                    launch(syncContext) {
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            database.transaction {
                                if (dbSong == null) {
                                    // Use proper MediaMetadata insertion to save artist information
                                    insert(song.toMediaMetadata()) { it.toggleLibrary() }
                                } else {
                                    if (dbSong.song.inLibrary == null) {
                                        update(dbSong.song.toggleLibrary())
                                    }
                                    addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncLikedAlbums() = coroutineScope {
        YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
            val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
            val remoteIds = remoteAlbums.map { it.id }.toSet()
            val localAlbums = database.albumsLikedByNameAsc().first()

            localAlbums.filterNot { it.id in remoteIds }
                .forEach { database.update(it.album.localToggleLike()) }

            remoteAlbums.forEach { album ->
                launch {
                    val dbAlbum = database.album(album.id).firstOrNull()
                    YouTube.album(album.browseId).onSuccess { albumPage ->
                        if (dbAlbum == null) {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                database.update(newDbAlbum.album.localToggleLike())
                            }
                        } else if (dbAlbum.album.bookmarkedAt == null) {
                            database.update(dbAlbum.album.localToggleLike())
                        }
                    }
                }
            }
        }
    }

    suspend fun syncArtistsSubscriptions() = coroutineScope {
        YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
            val remoteArtists = page.items.filterIsInstance<ArtistItem>()
            val remoteIds = remoteArtists.map { it.id }.toSet()
            val localArtists = database.artistsBookmarkedByNameAsc().first()

            localArtists.filterNot { it.id in remoteIds }
                .forEach { database.update(it.artist.localToggleLike()) }

            remoteArtists.forEach { artist ->
                launch {
                    val dbArtist = database.artist(artist.id).firstOrNull()
                    database.transaction {
                        if (dbArtist == null) {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        } else if (dbArtist.artist.bookmarkedAt == null) {
                            update(dbArtist.artist.localToggleLike())
                        }
                    }
                }
            }
        }
    }

    suspend fun syncSavedPlaylists() = coroutineScope {
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
            val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" }
                .reversed()
            val remoteIds = remotePlaylists.map { it.id }.toSet()
            val localPlaylists = database.playlistsByNameAsc().first()

            localPlaylists.filterNot { it.playlist.browseId in remoteIds }
                .filterNot { it.playlist.browseId == null }
                .forEach { database.update(it.playlist.localToggleLike()) }

            remotePlaylists.forEach { playlist ->
                launch {
                    var playlistEntity = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                    if (playlistEntity == null) {
                        playlistEntity = PlaylistEntity(
                            name = playlist.title,
                            browseId = playlist.id,
                            thumbnailUrl = playlist.thumbnail,
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let { Regex("""\\d+""").find(it)?.value?.toIntOrNull() },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                    } else {
                        // Update existing playlist with latest information including thumbnail
                        database.update(playlistEntity, playlist)
                    }
                    syncPlaylist(playlist.id, playlistEntity.id)
                }
            }
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        try {
            YouTube.playlist(browseId).completed().onSuccess { page ->
                val songs = page.songs.map(SongItem::toMediaMetadata)

                val remoteIds = songs.map { it.id }
                val localIds = database.playlistSongs(playlistId).first()
                    .sortedBy { it.map.position }
                    .map { it.song.id }

                if (remoteIds == localIds) return@onSuccess

                // Verify playlist exists before proceeding
                val playlistExists = database.playlist(playlistId).firstOrNull() != null
                if (!playlistExists) return@onSuccess

                database.transaction {
                    // Clear existing playlist songs
                    database.clearPlaylist(playlistId)
                    
                    // Insert songs first to ensure they exist
                    val songEntities = songs.onEach { song ->
                        if (runBlocking { database.song(song.id).firstOrNull() } == null) {
                            database.insert(song)
                        }
                    }
                    
                    // Create playlist song maps
                    val playlistSongMaps = songEntities.mapIndexed { position, song ->
                        PlaylistSongMap(
                            songId = song.id,
                            playlistId = playlistId,
                            position = position,
                            setVideoId = song.setVideoId
                        )
                    }
                    
                    // Insert all playlist song maps
                    playlistSongMaps.forEach { playlistSongMap ->
                        database.insert(playlistSongMap)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash the sync process
            e.printStackTrace()
        }
    }

    /**
     * Clears all YouTube Music synced content from the local database.
     * This includes:
     * - Liked songs (removes liked status)
     * - Library songs (removes library status)
     * - Liked albums (removes liked status)
     * - Subscribed artists (removes subscription)
     * - Saved playlists (removes playlists that have browseId)
     */
    suspend fun clearAllSyncedContent() = coroutineScope {
        try {
            // Get data outside of transaction to avoid long-running queries inside transaction
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()

            // Execute updates in smaller, separate transactions to avoid connection pool issues
            launch(syncContext) {
                // Clear liked songs
                likedSongs.forEach { songWithArtists ->
                    try {
                        database.transaction {
                            database.update(songWithArtists.song.copy(liked = false, likedDate = null))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            launch(syncContext) {
                // Clear library songs
                librarySongs.forEach { songWithArtists ->
                    if (songWithArtists.song.inLibrary != null) {
                        try {
                            database.transaction {
                                database.update(songWithArtists.song.copy(inLibrary = null))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            launch(syncContext) {
                // Clear liked albums
                likedAlbums.forEach { albumWithArtists ->
                    try {
                        database.transaction {
                            database.update(albumWithArtists.album.copy(bookmarkedAt = null))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            launch(syncContext) {
                // Clear subscribed artists
                subscribedArtists.forEach { artistWithSongs ->
                    try {
                        database.transaction {
                            database.update(artistWithSongs.artist.copy(bookmarkedAt = null))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            launch(syncContext) {
                // Clear saved playlists
                savedPlaylists.forEach { playlistWithSongs ->
                    if (playlistWithSongs.playlist.browseId != null) {
                        try {
                            database.transaction {
                                database.delete(playlistWithSongs.playlist)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
}
