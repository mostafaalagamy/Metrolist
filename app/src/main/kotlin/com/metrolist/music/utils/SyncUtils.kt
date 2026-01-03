/**
 * Metrolist Project (C) 2026
 * O‌ute‌rTu‌ne Project Copyright (C) 2025
 * Licensed under GPL-3.0 | See git history for contributors
 * 
 * Event-Driven Lazy Sync Strategy:
 * - No automatic sync on library screen entry (prevents UI jank)
 * - Sync only on: app startup (with cooldown), manual pull-to-refresh, explicit sync button
 * - Batch database operations to reduce UI recomposition
 * - Lower priority background processing
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.lastfm.LastFM
import com.metrolist.music.constants.LastFMUseSendLikes
import com.metrolist.music.constants.LastFullSyncKey
import com.metrolist.music.constants.SYNC_COOLDOWN
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.extensions.collectLatest
import com.metrolist.music.extensions.isInternetConnected
import com.metrolist.music.extensions.isSyncEnabled
import com.metrolist.music.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

// Increased delays for smoother UI - sync runs in true background
private const val SYNC_OPERATION_DELAY_MS = 1000L
// Larger batch delay to reduce database write frequency
private const val SYNC_BATCH_DELAY_MS = 200L
// Delay between processing individual items in a batch
private const val SYNC_ITEM_DELAY_MS = 50L

@OptIn(ExperimentalCoroutinesApi::class)
val syncCoroutine = Dispatchers.IO.limitedParallelism(1)

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val syncScope = CoroutineScope(syncCoroutine)

    private val isSyncingLikedSongs = MutableStateFlow(false)
    private val isSyncingUploadedSongs = MutableStateFlow(false)
    private val isSyncingLikedAlbums = MutableStateFlow(false)
    private val isSyncingUploadedAlbums = MutableStateFlow(false)
    private val isSyncingArtists = MutableStateFlow(false)
    private val isSyncingPlaylists = MutableStateFlow(false)
    private var lastfmSendLikes = false

    init {
        context.dataStore.data
            .map { it[LastFMUseSendLikes] ?: false }
            .distinctUntilChanged()
            .collectLatest(syncScope){
                lastfmSendLikes = it
            }
    }

    suspend fun tryAutoSync() {
        if (!context.isSyncEnabled() || !context.isInternetConnected()) {
            return
        }

        val lastSync = context.dataStore.get(LastFullSyncKey, 0L)
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (lastSync > 0 && (currentTime - lastSync) < SYNC_COOLDOWN) {
            return
        }

        // Add delays between sync operations to prevent blocking UI
        syncLikedSongs()
        delay(SYNC_OPERATION_DELAY_MS)
        yield() // Allow other coroutines to run
        
        syncUploadedSongs()
        delay(SYNC_OPERATION_DELAY_MS)
        yield()
        
        syncLikedAlbums()
        delay(SYNC_OPERATION_DELAY_MS)
        yield()
        
        syncUploadedAlbums()
        delay(SYNC_OPERATION_DELAY_MS)
        yield()
        
        syncArtistsSubscriptions()
        delay(SYNC_OPERATION_DELAY_MS)
        yield()
        
        syncSavedPlaylists()

        context.dataStore.edit { settings ->
            settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }
    }

    fun runAllSyncs() {
        syncScope.launch {
            syncLikedSongs()
            delay(SYNC_OPERATION_DELAY_MS)
            yield()
            
            syncUploadedSongs()
            delay(SYNC_OPERATION_DELAY_MS)
            yield()
            
            syncLikedAlbums()
            delay(SYNC_OPERATION_DELAY_MS)
            yield()
            
            syncUploadedAlbums()
            delay(SYNC_OPERATION_DELAY_MS)
            yield()
            
            syncArtistsSubscriptions()
            delay(SYNC_OPERATION_DELAY_MS)
            yield()
            
            syncSavedPlaylists()
        }
    }

    fun likeSong(s: SongEntity) {

        syncScope.launch {
            YouTube.likeVideo(s.id, s.liked)

            if (lastfmSendLikes) {
                val dbSong = database.song(s.id).firstOrNull()
                LastFM.setLoveStatus(
                    artist = dbSong?.artists?.joinToString { a -> a.name } ?: "",
                    track = s.title,
                    love = s.liked
                )
            }
        }
    }

    suspend fun syncLikedSongs() {
        if (isSyncingLikedSongs.value) return
        isSyncingLikedSongs.value = true
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remoteSongs = page.songs
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.likedSongsByNameAsc().first()

                // Process local songs not in remote - with batching
                localSongs.filterNot { it.id in remoteIds }.chunked(10).forEach { batch ->
                    batch.forEach {
                        try {
                            database.transaction { update(it.song.localToggleLike()) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    delay(SYNC_ITEM_DELAY_MS)
                    yield()
                }

                // Process remote songs - with batching
                remoteSongs.chunked(10).forEachIndexed { batchIndex, batch ->
                    batch.forEachIndexed { index, song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            val timestamp = LocalDateTime.now().minusSeconds((batchIndex * 10 + index).toLong())
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                                } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                    update(dbSong.song.copy(liked = true, likedDate = timestamp))
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    delay(SYNC_ITEM_DELAY_MS)
                    yield()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLikedSongs.value = false
        }
    }

    // COMMENTED OUT: Library sync function - disabled to save resources
    // suspend fun syncLibrarySongs() {
    //     if (isSyncingLibrarySongs.value) return
    //     isSyncingLibrarySongs.value = true
    //     try {
    //         YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
    //             val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
    //             val remoteIds = remoteSongs.map { it.id }.toSet()
    //             val localSongs = database.songsByNameAsc().first()
    //             val feedbackTokens = mutableListOf<String>()
    //
    //             localSongs.filterNot { it.id in remoteIds }.forEach {
    //                 if (it.song.libraryAddToken != null && it.song.libraryRemoveToken != null) {
    //                     feedbackTokens.add(it.song.libraryAddToken)
    //                 } else {
    //                     try {
    //                         database.transaction { update(it.song.toggleLibrary()) }
    //                     } catch (e: Exception) { e.printStackTrace() }
    //                 }
    //             }
    //             feedbackTokens.chunked(20).forEach { YouTube.feedback(it) }
    //
    //             remoteSongs.forEach { song ->
    //                 try {
    //                     val dbSong = database.song(song.id).firstOrNull()
    //                     database.transaction {
    //                         if (dbSong == null) {
    //                             insert(song.toMediaMetadata()) { it.toggleLibrary() }
    //                         } else {
    //                             if (dbSong.song.inLibrary == null) {
    //                                 update(dbSong.song.toggleLibrary())
    //                             }
    //                             addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
    //                         }
    //                     }
    //                 } catch (e: Exception) { e.printStackTrace() }
    //             }
    //         }
    //     } catch (e: Exception) {
    //         e.printStackTrace()
    //     } finally {
    //         isSyncingLibrarySongs.value = false
    //     }
    // }

    suspend fun syncUploadedSongs() {
        if (isSyncingUploadedSongs.value) return
        isSyncingUploadedSongs.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.uploadedSongsByNameAsc().first()

                localSongs.filterNot { it.id in remoteIds }.forEach { database.update(it.song.toggleUploaded()) }

                remoteSongs.forEach { song ->
                    val dbSong = database.song(song.id).firstOrNull()
                    database.transaction {
                        if (dbSong == null) {
                            insert(song.toMediaMetadata()) { it.toggleUploaded() }
                        } else if (!dbSong.song.isUploaded) {
                            update(dbSong.song.toggleUploaded())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingUploadedSongs.value = false
        }
    }

    suspend fun syncLikedAlbums() {
        if (isSyncingLikedAlbums.value) return
        isSyncingLikedAlbums.value = true
        try {
            YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsLikedByNameAsc().first()

                localAlbums.filterNot { it.id in remoteIds }.forEach { database.update(it.album.localToggleLike()) }

                remoteAlbums.forEach { album ->
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLikedAlbums.value = false
        }
    }

    suspend fun syncUploadedAlbums() {
        if (isSyncingUploadedAlbums.value) return
        isSyncingUploadedAlbums.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_releases", tabIndex = 1).completed().onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsUploadedByNameAsc().first()

                localAlbums.filterNot { it.id in remoteIds }.forEach { database.update(it.album.toggleUploaded()) }

                remoteAlbums.forEach { album ->
                    val dbAlbum = database.album(album.id).firstOrNull()
                    YouTube.album(album.browseId).onSuccess { albumPage ->
                        if (dbAlbum == null) {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                database.update(newDbAlbum.album.toggleUploaded())
                            }
                        } else if (!dbAlbum.album.isUploaded) {
                            database.update(dbAlbum.album.toggleUploaded())
                        }
                    }.onFailure { reportException(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingUploadedAlbums.value = false
        }
    }

    suspend fun syncArtistsSubscriptions() {
        if (isSyncingArtists.value) return
        isSyncingArtists.value = true
        try {
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
                val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                val remoteIds = remoteArtists.map { it.id }.toSet()
                val localArtists = database.artistsBookmarkedByNameAsc().first()

                localArtists.filterNot { it.id in remoteIds }.forEach { database.update(it.artist.localToggleLike()) }

                remoteArtists.forEach { artist ->
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingArtists.value = false
        }
    }

    /**
     * Sync all albums - both liked and uploaded (library albums)
     * Used by pull-to-refresh in LibraryAlbumsScreen
     */
    suspend fun syncAllAlbums() {
        syncLikedAlbums()
        delay(SYNC_OPERATION_DELAY_MS / 2)
        yield()
        syncUploadedAlbums()
    }

    /**
     * Sync all artists - subscriptions from library
     * Used by pull-to-refresh in LibraryArtistsScreen
     */
    suspend fun syncAllArtists() {
        syncArtistsSubscriptions()
    }

    suspend fun syncSavedPlaylists() {
        if (isSyncingPlaylists.value) return
        isSyncingPlaylists.value = true
        try {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                val remotePlaylists = page.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "LM" || it.id == "SE" }.reversed()
                val remoteIds = remotePlaylists.map { it.id }.toSet()
                val localPlaylists = database.playlistsByNameAsc().first()

                localPlaylists.filterNot { it.playlist.browseId in remoteIds }.filterNot { it.playlist.browseId == null }.forEach { database.update(it.playlist.localToggleLike()) }

                remotePlaylists.forEach { playlist ->
                    var playlistEntity = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                    if (playlistEntity == null) {
                        playlistEntity = PlaylistEntity(
                            name = playlist.title,
                            browseId = playlist.id,
                            thumbnailUrl = playlist.thumbnail,
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let {
                                Regex("""\d+""").find(it)?.value?.toIntOrNull()
                            },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                    } else {
                        database.update(playlistEntity, playlist)
                    }
                    syncPlaylist(playlist.id, playlistEntity.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) {
        try {
            YouTube.playlist(browseId).completed().onSuccess { page ->
                val songs = page.songs.map(SongItem::toMediaMetadata)
                val remoteIds = songs.map { it.id }
                val localIds = database.playlistSongs(playlistId).first().sortedBy { it.map.position }.map { it.song.id }

                if (remoteIds == localIds) return@onSuccess
                if (database.playlist(playlistId).firstOrNull() == null) return@onSuccess

                database.transaction {
                    clearPlaylist(playlistId)
                    // Insert songs directly - OnConflictStrategy.IGNORE handles duplicates
                    songs.forEach { song -> insert(song) }
                    // Create playlist song maps
                    songs.mapIndexed { position, song ->
                        PlaylistSongMap(songId = song.id, playlistId = playlistId, position = position, setVideoId = song.setVideoId)
                    }.forEach { insert(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllSyncedContent() {
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()
            val uploadedSongs = database.uploadedSongsByNameAsc().first()
            val uploadedAlbums = database.albumsUploadedByCreateDateAsc().first()

            likedSongs.forEach {
                try { database.transaction { update(it.song.copy(liked = false, likedDate = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            librarySongs.forEach {
                if (it.song.inLibrary != null) {
                    try { database.transaction { update(it.song.copy(inLibrary = null)) } } catch (e: Exception) { e.printStackTrace() }
                }
            }
            likedAlbums.forEach {
                try { database.transaction { update(it.album.copy(bookmarkedAt = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            subscribedArtists.forEach {
                try { database.transaction { update(it.artist.copy(bookmarkedAt = null)) } } catch (e: Exception) { e.printStackTrace() }
            }
            savedPlaylists.forEach {
                if (it.playlist.browseId != null) {
                    try { database.transaction { delete(it.playlist) } } catch (e: Exception) { e.printStackTrace() }
                }
            }
            // Clear uploaded content
            uploadedSongs.forEach {
                try { database.transaction { update(it.song.copy(isUploaded = false)) } } catch (e: Exception) { e.printStackTrace() }
            }
            uploadedAlbums.forEach {
                try { database.transaction { update(it.album.copy(isUploaded = false)) } } catch (e: Exception) { e.printStackTrace() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
