package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LastAlbumSyncKey
import com.metrolist.music.constants.LastArtistSyncKey
import com.metrolist.music.constants.LastLibSongSyncKey
import com.metrolist.music.constants.LastLikeSongSyncKey
import com.metrolist.music.constants.LastPlaylistSyncKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.extensions.isInternetConnected
import com.metrolist.music.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    private val COOLDOWN_SECONDS = 30 * 60L

    private val _syncingLikedSongs = MutableStateFlow(false)
    private val _syncingLibrarySongs = MutableStateFlow(false)
    private val _syncingAlbums = MutableStateFlow(false)
    private val _syncingArtists = MutableStateFlow(false)
    private val _syncingPlaylists = MutableStateFlow(false)

    val isSyncingLikedSongs = _syncingLikedSongs.asStateFlow()
    val isSyncingLibrarySongs = _syncingLibrarySongs.asStateFlow()
    val isSyncingLikedAlbums = _syncingAlbums.asStateFlow()
    val isSyncingArtists = _syncingArtists.asStateFlow()
    val isSyncingPlaylists = _syncingPlaylists.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tryAutoSync()
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data
                .map { prefs -> prefs[InnerTubeCookieKey] ?: "" }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { _ ->
                    resetAllTimestamps()
                    forceFullSyncAndWait()
                }
        }
    }

    private fun resetAllTimestamps() {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                prefs[LastLikeSongSyncKey] = 0L
                prefs[LastLibSongSyncKey] = 0L
                prefs[LastAlbumSyncKey] = 0L
                prefs[LastArtistSyncKey] = 0L
                prefs[LastPlaylistSyncKey] = 0L
            }
        }
    }

    private fun shouldSync(key: Preferences.Key<Long>, bypass: Boolean): Boolean {
        if (bypass) return true
        val last = context.dataStore.get(key, 0L)
        val now  = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        return (now - last) >= COOLDOWN_SECONDS
    }

    private suspend fun markSynced(key: Preferences.Key<Long>) {
        context.dataStore.edit { prefs ->
            prefs[key] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun likeSong(entity: SongEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            YouTube.likeVideo(entity.id, entity.liked)
        }
    }

    suspend fun tryAutoSync() {
        syncLikedSongs()
        syncLibrarySongs()
        syncLikedAlbums()
        syncArtistsSubscriptions()
        syncSavedPlaylists()
    }

    fun forceFullSync() {
        CoroutineScope(Dispatchers.IO).launch {
            syncLikedSongs(bypass = true)
            syncLibrarySongs(bypass = true)
            syncLikedAlbums(bypass = true)
            syncArtistsSubscriptions(bypass = true)
            syncSavedPlaylists(bypass = true)
        }
    }
    
    suspend fun forceFullSyncAndWait() = coroutineScope {
        val jobs = listOf(
            async { syncLikedSongs(bypass = true) },
            async { syncLibrarySongs(bypass = true) },
            async { syncLikedAlbums(bypass = true) },
            async { syncArtistsSubscriptions(bypass = true) },
            async { syncSavedPlaylists(bypass = true) }
        )
        jobs.awaitAll()
    }

    suspend fun syncLikedSongs(bypass: Boolean = false) {
        if (_syncingLikedSongs.value) return
        if (!context.isInternetConnected()) return
        if (!shouldSync(LastLikeSongSyncKey, bypass)) return

        _syncingLikedSongs.value = true
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remote = page.songs.reversed()
                val local = database.likedSongsByNameAsc().first()
                val rIds = remote.map { it.id }
                val now = LocalDateTime.now()
                
                coroutineScope {
                    remote.forEachIndexed { index, item ->
                        launch(Dispatchers.IO) {
                            val db = database.song(item.id).firstOrNull()
                            val timestamp = now.minusSeconds((remote.size - index - 1).toLong())
                            
                            if (db == null) {
                                database.insert(item.toMediaMetadata()
                                    .toSongEntity()
                                    .copy(liked = true, likedDate = timestamp))
                            } else if (!db.song.liked || db.song.likedDate != timestamp) {
                                database.update(db.song.copy(liked = true, likedDate = timestamp))
                            }
                        }
                    }
                    
                    local.filter { it.song.id !in rIds }
                         .forEach { s ->
                             launch(Dispatchers.IO) {
                                 database.update(s.song.localToggleLike())
                             }
                         }
                }
            }
            markSynced(LastLikeSongSyncKey)
        } finally {
            _syncingLikedSongs.value = false
        }
    }

    suspend fun syncLibrarySongs(bypass: Boolean = false) {
        if (_syncingLibrarySongs.value) return
        if (!context.isInternetConnected()) return
        if (!shouldSync(LastLibSongSyncKey, bypass)) return

        _syncingLibrarySongs.value = true
        try {
            YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
                val remote = page.items.filterIsInstance<SongItem>()
                val local = database.songsByNameAsc().first()
                val rIds = remote.map { it.id }.toSet()
                val now = LocalDateTime.now()
                
                coroutineScope {
                    remote.forEachIndexed { index, item ->
                        launch(Dispatchers.IO) {
                            val db = database.song(item.id).firstOrNull()
                            val timestamp = now.minusSeconds((remote.size - index - 1).toLong())
                            
                            if (db == null) {
                                val entity = item.toMediaMetadata().toSongEntity()
                                    .copy(inLibrary = timestamp)
                                database.insert(entity)
                            } else if (db.song.inLibrary == null || db.song.inLibrary != timestamp) {
                                database.update(db.song.copy(inLibrary = timestamp))
                            }
                        }
                    }
                    
                    local.filter { it.song.id !in rIds }
                         .forEach { s ->
                             launch(Dispatchers.IO) {
                                 database.update(s.song.toggleLibrary())
                             }
                         }
                }
            }
            markSynced(LastLibSongSyncKey)
        } finally {
            _syncingLibrarySongs.value = false
        }
    }

    suspend fun syncLikedAlbums(bypass: Boolean = false) {
        if (_syncingAlbums.value) return
        if (!context.isInternetConnected()) return
        if (!shouldSync(LastAlbumSyncKey, bypass)) return

        _syncingAlbums.value = true
        try {
            YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
                val remote = page.items.filterIsInstance<AlbumItem>()
                val local = database.albumsLikedByNameAsc().first()
                val rIds = remote.map { it.id }.toSet()
                val now = LocalDateTime.now()
                
                coroutineScope {
                    remote.forEachIndexed { index, item ->
                        launch(Dispatchers.IO) {
                            val timestamp = now.minusSeconds((remote.size - index - 1).toLong())
                            val db = database.album(item.id).firstOrNull()
                            
                            if (db == null || db.album.bookmarkedAt == null) {
                                YouTube.album(item.browseId).onSuccess { albumPage ->
                                    database.insert(albumPage)
                                    database.album(item.id).firstOrNull()?.let {
                                        database.update(it.album.copy(bookmarkedAt = timestamp))
                                    }
                                }
                            } else if (db.album.bookmarkedAt != timestamp) {
                                database.update(db.album.copy(bookmarkedAt = timestamp))
                            }
                        }
                    }
                    
                    local.filter { it.id !in rIds }
                         .forEach { a ->
                             launch(Dispatchers.IO) {
                                 database.update(a.album.localToggleLike())
                             }
                         }
                }
            }
            markSynced(LastAlbumSyncKey)
        } finally {
            _syncingAlbums.value = false
        }
    }

    suspend fun syncArtistsSubscriptions(bypass: Boolean = false) {
        if (_syncingArtists.value) return
        if (!context.isInternetConnected()) return
        if (!shouldSync(LastArtistSyncKey, bypass)) return

        _syncingArtists.value = true
        try {
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
                val remote = page.items.filterIsInstance<ArtistItem>()
                val local = database.artistsBookmarkedByNameAsc().first()
                val rIds = remote.map { it.id }.toSet()
                val now = LocalDateTime.now()
                
                coroutineScope {
                    remote.forEachIndexed { index, item ->
                        launch(Dispatchers.IO) {
                            val timestamp = now.minusSeconds((remote.size - index - 1).toLong())
                            val db = database.artist(item.id).firstOrNull()
                            
                            if (db == null) {
                                database.insert(
                                    ArtistEntity(
                                        id = item.id,
                                        name = item.title,
                                        thumbnailUrl = item.thumbnail,
                                        channelId = item.channelId,
                                        bookmarkedAt = timestamp
                                    )
                                )
                            } else if (db.artist.bookmarkedAt == null || db.artist.bookmarkedAt != timestamp) {
                                database.update(db.artist.copy(bookmarkedAt = timestamp))
                            }
                        }
                    }
                    
                    local.filter { it.id !in rIds }
                         .forEach { ar ->
                             launch(Dispatchers.IO) {
                                 database.update(ar.artist.localToggleLike())
                             }
                         }
                }
            }
            markSynced(LastArtistSyncKey)
        } finally {
            _syncingArtists.value = false
        }
    }

    suspend fun syncSavedPlaylists(bypass: Boolean = false) {
        if (_syncingPlaylists.value) return
        if (!context.isInternetConnected()) return
        if (!shouldSync(LastPlaylistSyncKey, bypass)) return

        _syncingPlaylists.value = true
        try {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                val remote = page.items.filterIsInstance<PlaylistItem>()
                                  .filterNot { it.id == "LM" || it.id == "SE" }
                val local = database.playlistsByNameAsc().first()
                val rIds = remote.map { it.id }.toSet()
                val now = LocalDateTime.now()
                
                coroutineScope {
                    remote.forEachIndexed { index, pl ->
                        launch(Dispatchers.IO) {
                            val timestamp = now.minusSeconds((remote.size - index - 1).toLong())
                            val exist = local.find { it.playlist.browseId == pl.id }?.playlist
                            
                            if (exist == null) {
                                val e = PlaylistEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = pl.title,
                                    browseId = pl.id,
                                    isEditable = pl.isEditable,
                                    bookmarkedAt = timestamp,
                                    remoteSongCount = pl.songCountText?.toIntOrNull(),
                                    playEndpointParams = pl.playEndpoint?.params,
                                    shuffleEndpointParams = pl.shuffleEndpoint?.params,
                                    radioEndpointParams = pl.radioEndpoint?.params
                                )
                                database.insert(e)
                                syncPlaylistContent(pl.id, e.id)
                            } else if (exist.bookmarkedAt != timestamp || 
                                      exist.name != pl.title || 
                                      exist.remoteSongCount != pl.songCountText?.toIntOrNull()) {
                                database.update(exist.copy(
                                    name = pl.title,
                                    bookmarkedAt = timestamp,
                                    remoteSongCount = pl.songCountText?.toIntOrNull()
                                ))
                                if (exist.isEditable) {
                                    syncPlaylistContent(pl.id, exist.id)
                                }
                            }
                        }
                    }
                    
                    local.filter { it.playlist.browseId !in rIds }
                         .forEach { lp ->
                             launch(Dispatchers.IO) {
                                 database.update(lp.playlist.localToggleLike())
                             }
                         }
                }
            }
            markSynced(LastPlaylistSyncKey)
        } finally {
            _syncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylistContent(browseId: String, playlistId: String) {
        YouTube.playlist(browseId).completed().onSuccess { page ->
            val songs = page.songs.map(SongItem::toMediaMetadata)
            
            val remoteIds = songs.map { it.id }
            val localIds = database.playlistSongs(playlistId).first()
                .sortedBy { it.map.position }
                .map { it.song.id }
                
            if (remoteIds == localIds) {
                return@onSuccess
            }
            
            coroutineScope {
                database.transaction {
                    runBlocking {
                        database.clearPlaylist(playlistId)
                        songs.forEachIndexed { idx, song ->
                            if (database.song(song.id).firstOrNull() == null) {
                                database.insert(song)
                            }
                            database.insert(PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistId,
                                position = idx,
                                setVideoId = song.setVideoId
                            ))
                        }
                    }
                }
            }
        }
    }
}
