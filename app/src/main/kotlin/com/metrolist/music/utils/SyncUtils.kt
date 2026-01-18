/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * 
 * Refactored SyncUtils - Queue-based synchronization with proper coroutine handling
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncOperation {
    data object FullSync : SyncOperation()
    data object LikedSongs : SyncOperation()
    data object LibrarySongs : SyncOperation()
    data object UploadedSongs : SyncOperation()
    data object LikedAlbums : SyncOperation()
    data object UploadedAlbums : SyncOperation()
    data object ArtistsSubscriptions : SyncOperation()
    data object SavedPlaylists : SyncOperation()
    data class SinglePlaylist(val browseId: String, val playlistId: String) : SyncOperation()
    data class LikeSong(val song: SongEntity) : SyncOperation()
    data object ClearAllSynced : SyncOperation()
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data object Completed : SyncStatus()
}

data class SyncState(
    val overallStatus: SyncStatus = SyncStatus.Idle,
    val likedSongs: SyncStatus = SyncStatus.Idle,
    val librarySongs: SyncStatus = SyncStatus.Idle,
    val uploadedSongs: SyncStatus = SyncStatus.Idle,
    val likedAlbums: SyncStatus = SyncStatus.Idle,
    val uploadedAlbums: SyncStatus = SyncStatus.Idle,
    val artists: SyncStatus = SyncStatus.Idle,
    val playlists: SyncStatus = SyncStatus.Idle,
    val currentOperation: String = ""
)

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Timber.e(throwable, "Sync coroutine exception")
        }
    }
    
    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(Dispatchers.IO + syncJob + exceptionHandler)
    
    private val syncChannel = Channel<SyncOperation>(Channel.BUFFERED)
    private var processingJob: Job? = null
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var lastfmSendLikes = false
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val DB_OPERATION_DELAY_MS = 50L
        private const val SYNC_OPERATION_DELAY_MS = 500L
    }

    init {
        context.dataStore.data
            .map { it[LastFMUseSendLikes] ?: false }
            .distinctUntilChanged()
            .collectLatest(syncScope) {
                lastfmSendLikes = it
            }
        
        startProcessingQueue()
    }
    
    private fun startProcessingQueue() {
        processingJob = syncScope.launch {
            for (operation in syncChannel) {
                try {
                    processOperation(operation)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error processing sync operation: $operation")
                }
            }
        }
    }
    
    private suspend fun processOperation(operation: SyncOperation) {
        when (operation) {
            is SyncOperation.FullSync -> executeFullSync()
            is SyncOperation.LikedSongs -> executeSyncLikedSongs()
            is SyncOperation.LibrarySongs -> executeSyncLibrarySongs()
            is SyncOperation.UploadedSongs -> executeSyncUploadedSongs()
            is SyncOperation.LikedAlbums -> executeSyncLikedAlbums()
            is SyncOperation.UploadedAlbums -> executeSyncUploadedAlbums()
            is SyncOperation.ArtistsSubscriptions -> executeSyncArtistsSubscriptions()
            is SyncOperation.SavedPlaylists -> executeSyncSavedPlaylists()
            is SyncOperation.SinglePlaylist -> executeSyncPlaylist(operation.browseId, operation.playlistId)
            is SyncOperation.LikeSong -> executeLikeSong(operation.song)
            is SyncOperation.ClearAllSynced -> executeClearAllSyncedContent()
        }
    }
    
    private suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_RETRY_DELAY_MS,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Attempt ${attempt + 1}/$maxRetries failed")
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }
    
    private fun updateState(update: SyncState.() -> SyncState) {
        _syncState.value = _syncState.value.update()
    }

    // Public API methods
    
    suspend fun tryAutoSync() {
        if (!context.isSyncEnabled() || !context.isInternetConnected()) {
            return
        }

        val lastSync = context.dataStore.get(LastFullSyncKey, 0L)
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (lastSync > 0 && (currentTime - lastSync) < SYNC_COOLDOWN) {
            return
        }

        syncChannel.send(SyncOperation.FullSync)

        context.dataStore.edit { settings ->
            settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        }
    }

    fun runAllSyncs() {
        syncScope.launch {
            syncChannel.send(SyncOperation.FullSync)
        }
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikeSong(s))
        }
    }
    
    suspend fun syncLikedSongs() {
        syncChannel.send(SyncOperation.LikedSongs)
    }
    
    suspend fun syncLibrarySongs() {
        syncChannel.send(SyncOperation.LibrarySongs)
    }
    
    suspend fun syncUploadedSongs() {
        syncChannel.send(SyncOperation.UploadedSongs)
    }
    
    suspend fun syncLikedAlbums() {
        syncChannel.send(SyncOperation.LikedAlbums)
    }
    
    suspend fun syncUploadedAlbums() {
        syncChannel.send(SyncOperation.UploadedAlbums)
    }
    
    suspend fun syncArtistsSubscriptions() {
        syncChannel.send(SyncOperation.ArtistsSubscriptions)
    }
    
    suspend fun syncSavedPlaylists() {
        syncChannel.send(SyncOperation.SavedPlaylists)
    }
    
    suspend fun clearAllSyncedContent() {
        syncChannel.send(SyncOperation.ClearAllSynced)
    }

    // Private execution methods
    
    private suspend fun executeFullSync() = withContext(Dispatchers.IO) {
        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Starting full sync") }
        
        try {
            executeSyncLikedSongs()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncLibrarySongs()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncUploadedSongs()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncLikedAlbums()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncUploadedAlbums()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncArtistsSubscriptions()
            delay(SYNC_OPERATION_DELAY_MS)
            
            executeSyncSavedPlaylists()
            
            updateState { copy(overallStatus = SyncStatus.Completed, currentOperation = "") }
            Timber.d("Full sync completed successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error during full sync")
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Unknown error"), currentOperation = "") }
        }
    }
    
    private suspend fun executeLikeSong(s: SongEntity) = withContext(Dispatchers.IO) {
        withRetry {
            YouTube.likeVideo(s.id, s.liked)
        }.onFailure { e ->
            Timber.e(e, "Failed to like song on YouTube: ${s.id}")
        }

        if (lastfmSendLikes) {
            try {
                val dbSong = database.song(s.id).firstOrNull()
                LastFM.setLoveStatus(
                    artist = dbSong?.artists?.joinToString { a -> a.name } ?: "",
                    track = s.title,
                    love = s.liked
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update LastFM love status")
            }
        }
    }

    private suspend fun executeSyncLikedSongs() = withContext(Dispatchers.IO) {
        updateState { copy(likedSongs = SyncStatus.Syncing, currentOperation = "Syncing liked songs") }
        
        withRetry {
            YouTube.playlist("LM").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.songs
                    if (remoteSongs.isEmpty()) {
                        Timber.d("No remote liked songs found")
                        updateState { copy(likedSongs = SyncStatus.Completed) }
                        return@onSuccess
                    }
                    
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.likedSongsByNameAsc().first()

                    localSongs.filterNot { it.id in remoteIds }.forEach { song ->
                        try {
                            database.transaction { update(song.song.localToggleLike()) }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update song: ${song.id}")
                        }
                    }

                    val totalSongs = remoteSongs.size
                    remoteSongs.reversed().forEachIndexed { index, song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            val positionFromEnd = totalSongs - 1 - index
                            val timestamp = LocalDateTime.now().minusSeconds(positionFromEnd.toLong())
                            val isVideoSong = song.isVideoSong
                            
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { 
                                        it.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong) 
                                    }
                                } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp || dbSong.song.isVideo != isVideoSong) {
                                    update(dbSong.song.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong))
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process song: ${song.id}")
                        }
                    }
                    
                    updateState { copy(likedSongs = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteSongs.size} liked songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing liked songs")
                    updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch liked songs from YouTube")
                updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync liked songs after retries")
            updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncLibrarySongs() = withContext(Dispatchers.IO) {
        updateState { copy(librarySongs = SyncStatus.Syncing, currentOperation = "Syncing library songs") }
        
        withRetry {
            YouTube.library("FEmusic_liked_videos").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.items.filterIsInstance<SongItem>()
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.songsByNameAsc().first()
                    val feedbackTokens = mutableListOf<String>()

                    localSongs.filterNot { it.id in remoteIds }.forEach { song ->
                        if (song.song.libraryAddToken != null && song.song.libraryRemoveToken != null) {
                            feedbackTokens.add(song.song.libraryAddToken)
                        } else {
                            try {
                                database.transaction { update(song.song.toggleLibrary()) }
                                delay(DB_OPERATION_DELAY_MS)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to update song: ${song.id}")
                            }
                        }
                    }
                    
                    feedbackTokens.chunked(20).forEach { 
                        try { YouTube.feedback(it) } catch (e: Exception) { Timber.e(e, "Feedback failed") }
                    }

                    val totalSongs = remoteSongs.size
                    remoteSongs.reversed().forEachIndexed { index, song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            val positionFromEnd = totalSongs - 1 - index
                            val timestamp = LocalDateTime.now().minusSeconds(positionFromEnd.toLong())
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { it.copy(inLibrary = timestamp) }
                                } else {
                                    if (dbSong.song.inLibrary == null) {
                                        update(dbSong.song.copy(inLibrary = timestamp))
                                    }
                                    addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process song: ${song.id}")
                        }
                    }
                    
                    updateState { copy(librarySongs = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteSongs.size} library songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing library songs")
                    updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch library songs from YouTube")
                updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync library songs after retries")
            updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncUploadedSongs() = withContext(Dispatchers.IO) {
        updateState { copy(uploadedSongs = SyncStatus.Syncing, currentOperation = "Syncing uploaded songs") }
        
        withRetry {
            YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.uploadedSongsByNameAsc().first()

                    localSongs.filterNot { it.id in remoteIds }.forEach { song ->
                        try {
                            database.update(song.song.toggleUploaded())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update song: ${song.id}")
                        }
                    }

                    remoteSongs.forEach { song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { it.toggleUploaded() }
                                } else if (!dbSong.song.isUploaded) {
                                    update(dbSong.song.toggleUploaded())
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process song: ${song.id}")
                        }
                    }
                    
                    updateState { copy(uploadedSongs = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteSongs.size} uploaded songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing uploaded songs")
                    updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch uploaded songs from YouTube")
                updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync uploaded songs after retries")
            updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncLikedAlbums() = withContext(Dispatchers.IO) {
        updateState { copy(likedAlbums = SyncStatus.Syncing, currentOperation = "Syncing liked albums") }
        
        withRetry {
            YouTube.library("FEmusic_liked_albums").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = database.albumsLikedByNameAsc().first()

                    localAlbums.filterNot { it.id in remoteIds }.forEach { album ->
                        try {
                            database.update(album.album.localToggleLike())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update album: ${album.id}")
                        }
                    }

                    remoteAlbums.forEach { album ->
                        try {
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
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process album: ${album.id}")
                        }
                    }
                    
                    updateState { copy(likedAlbums = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteAlbums.size} liked albums")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing liked albums")
                    updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch liked albums from YouTube")
                updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync liked albums after retries")
            updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncUploadedAlbums() = withContext(Dispatchers.IO) {
        updateState { copy(uploadedAlbums = SyncStatus.Syncing, currentOperation = "Syncing uploaded albums") }
        
        withRetry {
            YouTube.library("FEmusic_library_privately_owned_releases", tabIndex = 1).completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = database.albumsUploadedByNameAsc().first()

                    localAlbums.filterNot { it.id in remoteIds }.forEach { album ->
                        try {
                            database.update(album.album.toggleUploaded())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update album: ${album.id}")
                        }
                    }

                    remoteAlbums.forEach { album ->
                        try {
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
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process album: ${album.id}")
                        }
                    }
                    
                    updateState { copy(uploadedAlbums = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteAlbums.size} uploaded albums")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing uploaded albums")
                    updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch uploaded albums from YouTube")
                updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync uploaded albums after retries")
            updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncArtistsSubscriptions() = withContext(Dispatchers.IO) {
        updateState { copy(artists = SyncStatus.Syncing, currentOperation = "Syncing artist subscriptions") }
        
        withRetry {
            YouTube.library("FEmusic_library_corpus_artists").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                    val remoteIds = remoteArtists.map { it.id }.toSet()
                    val localArtists = database.artistsBookmarkedByNameAsc().first()

                    localArtists.filterNot { it.id in remoteIds }.forEach { artist ->
                        try {
                            database.update(artist.artist.localToggleLike())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update artist: ${artist.id}")
                        }
                    }

                    remoteArtists.forEach { artist ->
                        try {
                            val dbArtist = database.artist(artist.id).firstOrNull()
                            val channelId = artist.channelId ?: if (artist.id.startsWith("UC")) {
                                try {
                                    YouTube.getChannelId(artist.id).takeIf { it.isNotEmpty() }
                                } catch (e: Exception) {
                                    null
                                }
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
                                    val needsChannelIdUpdate = dbArtist.artist.channelId == null && channelId != null
                                    if (dbArtist.artist.bookmarkedAt == null || needsChannelIdUpdate) {
                                        update(dbArtist.artist.copy(
                                            bookmarkedAt = dbArtist.artist.bookmarkedAt ?: LocalDateTime.now(),
                                            channelId = channelId ?: dbArtist.artist.channelId
                                        ))
                                    }
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process artist: ${artist.id}")
                        }
                    }
                    
                    updateState { copy(artists = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteArtists.size} artist subscriptions")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing artist subscriptions")
                    updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch artist subscriptions from YouTube")
                updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync artist subscriptions after retries")
            updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncSavedPlaylists() = withContext(Dispatchers.IO) {
        updateState { copy(playlists = SyncStatus.Syncing, currentOperation = "Syncing saved playlists") }
        
        withRetry {
            YouTube.library("FEmusic_liked_playlists").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .reversed()
                    val remoteIds = remotePlaylists.map { it.id }.toSet()
                    val localPlaylists = database.playlistsByNameAsc().first()

                    localPlaylists.filterNot { it.playlist.browseId in remoteIds }
                        .filterNot { it.playlist.browseId == null }
                        .forEach { playlist ->
                            try {
                                database.update(playlist.playlist.localToggleLike())
                                delay(DB_OPERATION_DELAY_MS)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to update playlist: ${playlist.id}")
                            }
                        }

                    for (playlist in remotePlaylists) {
                        try {
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
                                Timber.d("Created new playlist: ${playlist.title}")
                            } else {
                                database.update(playlistEntity, playlist)
                            }
                            
                            executeSyncPlaylist(playlist.id, playlistEntity.id)
                            delay(SYNC_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to sync playlist ${playlist.title}")
                        }
                    }
                    
                    updateState { copy(playlists = SyncStatus.Completed) }
                    Timber.d("Synced ${remotePlaylists.size} saved playlists")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing saved playlists")
                    updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch playlists from YouTube")
                updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync saved playlists after retries")
            updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncPlaylist(browseId: String, playlistId: String) = withContext(Dispatchers.IO) {
        Timber.d("Syncing playlist: browseId=$browseId, playlistId=$playlistId")
        
        withRetry {
            YouTube.playlist(browseId).completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val songs = page.songs.map(SongItem::toMediaMetadata)
                    
                    if (songs.isEmpty()) {
                        Timber.d("Remote playlist is empty, skipping sync")
                        return@onSuccess
                    }
                    
                    val remoteIds = songs.map { it.id }
                    val localIds = database.playlistSongs(playlistId).first()
                        .sortedBy { it.map.position }
                        .map { it.song.id }

                    if (remoteIds == localIds) {
                        Timber.d("Playlist already in sync")
                        return@onSuccess
                    }
                    
                    if (database.playlist(playlistId).firstOrNull() == null) {
                        Timber.w("Playlist not found in database: $playlistId")
                        return@onSuccess
                    }

                    database.transaction {
                        clearPlaylist(playlistId)
                        songs.forEach { song -> insert(song) }
                        songs.mapIndexed { position, song ->
                            PlaylistSongMap(
                                songId = song.id, 
                                playlistId = playlistId, 
                                position = position, 
                                setVideoId = song.setVideoId
                            )
                        }.forEach { insert(it) }
                    }
                    
                    Timber.d("Successfully synced playlist with ${songs.size} songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing playlist sync")
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch playlist from YouTube")
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync playlist after retries")
        }
    }

    private suspend fun executeClearAllSyncedContent() = withContext(Dispatchers.IO) {
        Timber.d("Clearing all synced content")
        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Clearing synced content") }
        
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()
            val uploadedSongs = database.uploadedSongsByNameAsc().first()
            val uploadedAlbums = database.albumsUploadedByCreateDateAsc().first()

            likedSongs.forEach {
                try { 
                    database.transaction { update(it.song.copy(liked = false, likedDate = null)) } 
                } catch (e: Exception) { 
                    Timber.e(e, "Failed to clear liked song: ${it.id}") 
                }
            }
            
            librarySongs.forEach {
                if (it.song.inLibrary != null) {
                    try { 
                        database.transaction { update(it.song.copy(inLibrary = null)) } 
                    } catch (e: Exception) { 
                        Timber.e(e, "Failed to clear library song: ${it.id}") 
                    }
                }
            }
            
            likedAlbums.forEach {
                try { 
                    database.transaction { update(it.album.copy(bookmarkedAt = null)) } 
                } catch (e: Exception) { 
                    Timber.e(e, "Failed to clear liked album: ${it.id}") 
                }
            }
            
            subscribedArtists.forEach {
                try { 
                    database.transaction { update(it.artist.copy(bookmarkedAt = null)) } 
                } catch (e: Exception) { 
                    Timber.e(e, "Failed to clear subscribed artist: ${it.id}") 
                }
            }
            
            savedPlaylists.forEach {
                if (it.playlist.browseId != null) {
                    try { 
                        database.transaction { delete(it.playlist) } 
                    } catch (e: Exception) { 
                        Timber.e(e, "Failed to delete playlist: ${it.id}") 
                    }
                }
            }
            
            uploadedSongs.forEach {
                try { 
                    database.transaction { update(it.song.copy(isUploaded = false)) } 
                } catch (e: Exception) { 
                    Timber.e(e, "Failed to clear uploaded song: ${it.id}") 
                }
            }
            
            uploadedAlbums.forEach {
                try { 
                    database.transaction { update(it.album.copy(isUploaded = false)) } 
                } catch (e: Exception) { 
                    Timber.e(e, "Failed to clear uploaded album: ${it.id}") 
                }
            }
            
            updateState { copy(overallStatus = SyncStatus.Completed, currentOperation = "") }
            Timber.d("Successfully cleared all synced content")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing synced content")
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Unknown error"), currentOperation = "") }
        }
    }
    
    fun cancelAllSyncs() {
        processingJob?.cancel()
        startProcessingQueue()
        updateState { SyncState() }
    }
}
