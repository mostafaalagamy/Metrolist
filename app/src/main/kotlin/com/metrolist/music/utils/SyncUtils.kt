/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
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
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.lastfm.LastFM
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LastFMUseSendLikes
import com.metrolist.music.constants.LastFullSyncKey
import com.metrolist.music.constants.SelectedYtmPlaylistsKey
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    private val syncMutex = Mutex()
    private val playlistSyncMutex = Mutex()
    private val dbWriteSemaphore = Semaphore(2)
    private val isSyncing = AtomicBoolean(false)
    
    private val isSyncingLikedSongs = MutableStateFlow(false)
    private val isSyncingLibrarySongs = MutableStateFlow(false)
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
            .collectLatest(syncScope) {
                lastfmSendLikes = it
            }
    }

    private suspend fun isLoggedIn(): Boolean {
        val cookie = context.dataStore.data
            .map { it[InnerTubeCookieKey] }
            .first()
        return cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
    }

    suspend fun performFullSync() = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Timber.d("Sync already in progress, skipping")
            return@withContext
        }
        
        try {
            syncMutex.withLock {
                if (!isLoggedIn()) {
                    Timber.w("Skipping full sync - user not logged in")
                    return@withLock
                }
                
                supervisorScope {
                    listOf(
                        async { syncLikedSongs() },
                        async { syncLibrarySongs() },
                        async { syncLikedAlbums() },
                        async { syncArtistsSubscriptions() },
                    ).awaitAll()
                    
                    syncSavedPlaylists()
                    syncAutoSyncPlaylists()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during full sync")
        } finally {
            isSyncing.set(false)
        }
    }

    suspend fun tryAutoSync() {
        if (!isLoggedIn()) {
            Timber.d("Skipping auto sync - user not logged in")
            return
        }

        if (!context.isSyncEnabled() || !context.isInternetConnected()) {
            return
        }

        val lastSync = context.dataStore.get(LastFullSyncKey, 0L)
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (lastSync > 0 && (currentTime - lastSync) < SYNC_COOLDOWN) {
            return
        }

        performFullSync()

        context.dataStore.edit { settings ->
            settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }
    }

    fun runAllSyncs() {
        syncScope.launch {
            performFullSync()
        }
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            if (!isLoggedIn()) {
                Timber.w("Skipping likeSong - user not logged in")
                return@launch
            }
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

    suspend fun syncLikedSongs() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedSongs - user not logged in")
            return@coroutineScope
        }
        YouTube.playlist("LM").completed().onSuccess { page ->
            val remoteSongs = page.songs
            val remoteIds = remoteSongs.map { it.id }
            val localSongs = database.likedSongsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.localToggleLike()) }

            val now = LocalDateTime.now()
            remoteSongs.forEachIndexed { index, song ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        val isVideoSong = song.isVideoSong
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong) }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp || dbSong.song.isVideo != isVideoSong) {
                                update(dbSong.song.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong))
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLibrarySongs - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
            val remoteIds = remoteSongs.map { it.id }.toSet()
            val localSongs = database.songsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.toggleLibrary()) }

            remoteSongs.forEach { song ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbSong = database.song(song.id).firstOrNull()
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else if (dbSong.song.inLibrary == null) {
                                update(dbSong.song.toggleLibrary())
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncUploadedSongs() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncUploadedSongs - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
            val remoteIds = remoteSongs.map { it.id }.toSet()
            val localSongs = database.uploadedSongsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.toggleUploaded()) }

            remoteSongs.forEach { song ->
                launch {
                    dbWriteSemaphore.withPermit {
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
            }
        }
    }

    suspend fun syncLikedAlbums() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedAlbums - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
            val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
            val remoteIds = remoteAlbums.map { it.id }.toSet()
            val localAlbums = database.albumsLikedByNameAsc().first()

            localAlbums.filterNot { it.id in remoteIds }
                .forEach { database.update(it.album.localToggleLike()) }

            remoteAlbums.forEach { album ->
                launch {
                    dbWriteSemaphore.withPermit {
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
    }

    suspend fun syncUploadedAlbums() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncUploadedAlbums - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_library_privately_owned_releases", tabIndex = 1).completed().onSuccess { page ->
            val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
            val remoteIds = remoteAlbums.map { it.id }.toSet()
            val localAlbums = database.albumsUploadedByNameAsc().first()

            localAlbums.filterNot { it.id in remoteIds }
                .forEach { database.update(it.album.toggleUploaded()) }

            remoteAlbums.forEach { album ->
                launch {
                    dbWriteSemaphore.withPermit {
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
            }
        }
    }

    suspend fun syncArtistsSubscriptions() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncArtistsSubscriptions - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
            val remoteArtists = page.items.filterIsInstance<ArtistItem>()
            val remoteIds = remoteArtists.map { it.id }.toSet()
            val localArtists = database.artistsBookmarkedByNameAsc().first()

            localArtists.filterNot { it.id in remoteIds }
                .forEach { database.update(it.artist.localToggleLike()) }

            remoteArtists.forEach { artist ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbArtist = database.artist(artist.id).firstOrNull()
                        val channelId = artist.channelId ?: if (artist.id.startsWith("UC")) {
                            YouTube.getChannelId(artist.id).takeIf { it.isNotEmpty() }
                        } else null
                        database.transaction {
                            if (dbArtist == null) {
                                insert(
                                    ArtistEntity(
                                        id = artist.id,
                                        name = artist.title,
                                        thumbnailUrl = artist.thumbnail,
                                        channelId = channelId,
                                        bookmarkedAt = LocalDateTime.now()
                                    )
                                )
                            } else {
                                val existing = dbArtist.artist
                                val needsChannelIdUpdate = existing.channelId == null && channelId != null
                                if (existing.bookmarkedAt == null || needsChannelIdUpdate ||
                                    existing.name != artist.title || existing.thumbnailUrl != artist.thumbnail) {
                                    update(
                                        existing.copy(
                                            name = artist.title,
                                            thumbnailUrl = artist.thumbnail,
                                            channelId = channelId ?: existing.channelId,
                                            bookmarkedAt = existing.bookmarkedAt ?: LocalDateTime.now(),
                                            lastUpdateTime = LocalDateTime.now()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncAllAlbums() {
        syncLikedAlbums()
        syncUploadedAlbums()
    }

    suspend fun syncAllArtists() {
        syncArtistsSubscriptions()
    }

    suspend fun syncSavedPlaylists() = playlistSyncMutex.withLock {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncSavedPlaylists - user not logged in")
            return@withLock
        }
        
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
            val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" }
                .reversed()

            val selectedCsv = context.dataStore[SelectedYtmPlaylistsKey] ?: ""
            val selectedIds = selectedCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            val playlistsToSync = if (selectedIds.isNotEmpty()) remotePlaylists.filter { it.id in selectedIds } else remotePlaylists
            val remoteIds = playlistsToSync.map { it.id }.toSet()

            val localPlaylists = database.playlistsByNameAsc().first()
            localPlaylists.filterNot { it.playlist.browseId in remoteIds }
                .filterNot { it.playlist.browseId == null }
                .forEach { database.update(it.playlist.localToggleLike()) }

            for (playlist in playlistsToSync) {
                try {
                    val existingPlaylist = database.playlistByBrowseId(playlist.id).firstOrNull()
                    
                    val playlistEntity: PlaylistEntity
                    if (existingPlaylist == null) {
                        playlistEntity = PlaylistEntity(
                            name = playlist.title,
                            browseId = playlist.id,
                            thumbnailUrl = playlist.thumbnail,
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                        Timber.d("syncSavedPlaylists: Created new playlist ${playlist.title} (${playlist.id})")
                    } else {
                        playlistEntity = existingPlaylist.playlist
                        database.update(playlistEntity, playlist)
                        Timber.d("syncSavedPlaylists: Updated existing playlist ${playlist.title} (${playlist.id})")
                    }
                    
                    syncPlaylist(playlist.id, playlistEntity.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync playlist ${playlist.title}")
                }
            }
        }.onFailure { e ->
            Timber.e(e, "syncSavedPlaylists: Failed to fetch playlists from YouTube")
        }
    }

    suspend fun syncAutoSyncPlaylists() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncAutoSyncPlaylists - user not logged in")
            return@coroutineScope
        }
        val autoSyncPlaylists = database.playlistsByNameAsc().first()
            .filter { it.playlist.isAutoSync && it.playlist.browseId != null }

        Timber.d("syncAutoSyncPlaylists: Found ${autoSyncPlaylists.size} playlists to sync")

        autoSyncPlaylists.forEach { playlist ->
            launch {
                try {
                    dbWriteSemaphore.withPermit {
                        syncPlaylist(playlist.playlist.browseId!!, playlist.playlist.id)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync playlist ${playlist.playlist.name}")
                }
            }
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        Timber.d("syncPlaylist: Starting sync for browseId=$browseId, playlistId=$playlistId")
        
        YouTube.playlist(browseId).completed().onSuccess { page ->
            val songs = page.songs.map(SongItem::toMediaMetadata)
            Timber.d("syncPlaylist: Fetched ${songs.size} songs from remote")

            if (songs.isEmpty()) {
                Timber.w("syncPlaylist: Remote playlist is empty, skipping sync")
                return@onSuccess
            }

            val remoteIds = songs.map { it.id }
            val localIds = database.playlistSongs(playlistId).first()
                .sortedBy { it.map.position }
                .map { it.song.id }

            if (remoteIds == localIds) {
                Timber.d("syncPlaylist: Local and remote are in sync, no changes needed")
                return@onSuccess
            }

            Timber.d("syncPlaylist: Updating local playlist (remote: ${remoteIds.size}, local: ${localIds.size})")

            database.withTransaction {
                database.clearPlaylist(playlistId)
                songs.forEachIndexed { idx, song ->
                    if (database.song(song.id).firstOrNull() == null) {
                        database.insert(song)
                    }
                    database.insert(
                        PlaylistSongMap(
                            songId = song.id,
                            playlistId = playlistId,
                            position = idx,
                            setVideoId = song.setVideoId
                        )
                    )
                }
            }
            Timber.d("syncPlaylist: Successfully synced playlist")
        }.onFailure { e ->
            Timber.e(e, "syncPlaylist: Failed to fetch playlist from YouTube")
        }
    }

    suspend fun cleanupDuplicatePlaylists() = withContext(Dispatchers.IO) {
        try {
            val allPlaylists = database.playlistsByNameAsc().first()
            val browseIdGroups = allPlaylists
                .filter { it.playlist.browseId != null }
                .groupBy { it.playlist.browseId }
            
            for ((browseId, playlists) in browseIdGroups) {
                if (playlists.size > 1) {
                    Timber.w("Found ${playlists.size} duplicate playlists for browseId: $browseId")
                    val toKeep = playlists.maxByOrNull { it.songCount }
                        ?: playlists.first()
                    
                    playlists.filter { it.id != toKeep.id }.forEach { duplicate ->
                        Timber.d("Removing duplicate playlist: ${duplicate.playlist.name} (${duplicate.id})")
                        database.clearPlaylist(duplicate.id)
                        database.delete(duplicate.playlist)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up duplicate playlists")
        }
    }

    suspend fun clearAllSyncedContent() = syncMutex.withLock {
        Timber.d("clearAllSyncedContent: Starting cleanup")
        
        // Wait for any ongoing sync to complete
        while (isSyncing.get()) {
            Timber.d("clearAllSyncedContent: Waiting for sync to complete...")
            kotlinx.coroutines.delay(100)
        }
        
        // Set syncing flag to prevent new syncs from starting
        isSyncing.set(true)
        
        try {
            database.withTransaction {
                // Clear liked songs
                val likedSongs = database.likedSongsByNameAsc().first()
                likedSongs.forEach {
                    database.update(it.song.copy(liked = false, likedDate = null))
                }
                
                // Clear library songs
                val librarySongs = database.songsByNameAsc().first()
                librarySongs.forEach {
                    if (it.song.inLibrary != null) {
                        database.update(it.song.copy(inLibrary = null))
                    }
                }
                
                // Clear liked albums
                val likedAlbums = database.albumsLikedByNameAsc().first()
                likedAlbums.forEach {
                    database.update(it.album.copy(bookmarkedAt = null))
                }
                
                // Clear subscribed artists
                val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
                subscribedArtists.forEach {
                    database.update(it.artist.copy(bookmarkedAt = null))
                }
                
                // Delete synced playlists
                val savedPlaylists = database.playlistsByNameAsc().first()
                savedPlaylists.forEach {
                    if (it.playlist.browseId != null) {
                        database.clearPlaylist(it.playlist.id)
                        database.delete(it.playlist)
                    }
                }
                
                // Clear uploaded songs
                val uploadedSongs = database.uploadedSongsByNameAsc().first()
                uploadedSongs.forEach {
                    database.update(it.song.copy(isUploaded = false))
                }
                
                // Clear uploaded albums
                val uploadedAlbums = database.albumsUploadedByCreateDateAsc().first()
                uploadedAlbums.forEach {
                    database.update(it.album.copy(isUploaded = false))
                }
            }
            
            // Reset sync timestamp to prevent immediate re-sync
            context.dataStore.edit { settings ->
                settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            }
            
            // Clear selected playlists preference
            context.dataStore.edit { settings ->
                settings.remove(SelectedYtmPlaylistsKey)
            }

            Timber.d("clearAllSyncedContent: Cleanup completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "clearAllSyncedContent: Error during cleanup")
            e.printStackTrace()
        } finally {
            isSyncing.set(false)
        }
    }
}
