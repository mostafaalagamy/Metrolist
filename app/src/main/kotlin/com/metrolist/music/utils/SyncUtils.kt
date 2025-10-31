package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.LastWhitelistHashKey
import com.metrolist.music.constants.LastWhitelistSyncTimeKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class WhitelistSyncProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentArtistName: String = "",
    val isComplete: Boolean = false
)

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val isSyncingLikedSongs = MutableStateFlow(false)
    private val isSyncingLibrarySongs = MutableStateFlow(false)
    private val isSyncingUploadedSongs = MutableStateFlow(false)
    private val isSyncingLikedAlbums = MutableStateFlow(false)
    private val isSyncingUploadedAlbums = MutableStateFlow(false)
    private val isSyncingArtists = MutableStateFlow(false)
    private val isSyncingPlaylists = MutableStateFlow(false)
    private val isSyncingWhitelist = MutableStateFlow(false)

    private val _whitelistSyncProgress = MutableStateFlow(WhitelistSyncProgress())
    val whitelistSyncProgress: StateFlow<WhitelistSyncProgress> = _whitelistSyncProgress.asStateFlow()

    fun runAllSyncs() {
        syncScope.launch {
            syncArtistWhitelist()
            syncLikedSongs()
            syncLibrarySongs()
            syncUploadedSongs()
            syncLikedAlbums()
            syncUploadedAlbums()
            syncArtistsSubscriptions()
            syncSavedPlaylists()
        }
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs() {
        if (isSyncingLikedSongs.value) return
        isSyncingLikedSongs.value = true
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remoteSongs = page.songs
                val remoteIds = remoteSongs.map { it.id }
                val localSongs = database.likedSongsByNameAsc().first()

                localSongs.filterNot { it.id in remoteIds }.forEach {
                    try {
                        database.transaction { update(it.song.localToggleLike()) }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                remoteSongs.forEachIndexed { index, song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                update(dbSong.song.copy(liked = true, likedDate = timestamp))
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLikedSongs.value = false
        }
    }

    suspend fun syncLibrarySongs() {
        if (isSyncingLibrarySongs.value) return
        isSyncingLibrarySongs.value = true
        try {
            YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.songsByNameAsc().first()
                val feedbackTokens = mutableListOf<String>()

                localSongs.filterNot { it.id in remoteIds }.forEach {
                    if (it.song.libraryAddToken != null && it.song.libraryRemoveToken != null) {
                        feedbackTokens.add(it.song.libraryAddToken)
                    } else {
                        try {
                            database.transaction { update(it.song.toggleLibrary()) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                feedbackTokens.chunked(20).forEach { YouTube.feedback(it) }

                remoteSongs.forEach { song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else {
                                if (dbSong.song.inLibrary == null) {
                                    update(dbSong.song.toggleLibrary())
                                }
                                addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLibrarySongs.value = false
        }
    }

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
                    val songEntities = songs.onEach { song ->
                        if (runBlocking { database.song(song.id).firstOrNull() } == null) {
                            insert(song)
                        }
                    }
                    val playlistSongMaps = songEntities.mapIndexed { position, song ->
                        PlaylistSongMap(songId = song.id, playlistId = playlistId, position = position, setVideoId = song.setVideoId)
                    }
                    playlistSongMaps.forEach { insert(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncArtistWhitelist(forceSync: Boolean = false) {
        if (isSyncingWhitelist.value) return

        // Fetch JSON and check hash to detect changes
        if (!forceSync) {
            Timber.d("Whitelist sync: Fetching JSON to check for changes...")

            WhitelistFetcher.fetchWhitelistHash().onSuccess { (jsonString, newHash) ->
                val storedHash = context.dataStore.data.first()[LastWhitelistHashKey]

                if (storedHash == newHash) {
                    // Hash matches - no changes, skip sync
                    Timber.d("Whitelist sync: No changes detected (hash matches), skipping sync")
                    _whitelistSyncProgress.value = WhitelistSyncProgress(isComplete = true)
                    return
                }

                // Hash differs or no stored hash - proceed with sync
                Timber.d("Whitelist sync: Changes detected (hash: $newHash), proceeding with sync")
                isSyncingWhitelist.value = true

                // Reset progress
                _whitelistSyncProgress.value = WhitelistSyncProgress()

                try {
                    Timber.d("Whitelist sync starting...")
                    val whitelistEntries = WhitelistFetcher.parseWhitelistJson(jsonString)
                Timber.d("Whitelist sync: Fetched ${whitelistEntries.size} artists from GitHub")

                // Update progress with total count
                _whitelistSyncProgress.value = WhitelistSyncProgress(
                    total = whitelistEntries.size
                )

                // Get current whitelist IDs before clearing (for deletion comparison)
                val currentWhitelistIds = database.getAllWhitelistedArtistIdsSync()
                val newWhitelistIds = whitelistEntries.map { it.artistId }.toSet()
                val removedArtistIds = currentWhitelistIds.filterNot { it in newWhitelistIds }

                if (removedArtistIds.isNotEmpty()) {
                    Timber.d("Whitelist sync: Detected ${removedArtistIds.size} removed artists, deleting their content")
                    deleteRemovedArtists(removedArtistIds)
                } else {
                    Timber.d("Whitelist sync: No artists removed from whitelist")
                }

                database.transaction {
                    clearWhitelist()
                    insertWhitelist(whitelistEntries)
                }
                Timber.d("Whitelist sync: Successfully synced ${whitelistEntries.size} artists to whitelist table")

                // Filter out artists that already have thumbnails (smart caching)
                val artistsToFetch = whitelistEntries.mapNotNull { entry ->
                    val existingArtist = database.artist(entry.artistId).firstOrNull()
                    if (existingArtist?.artist?.thumbnailUrl == null) {
                        entry to existingArtist
                    } else {
                        // Artist already has thumbnail, skip
                        Timber.d("Whitelist sync: Skipping ${existingArtist.artist.name} - thumbnail already exists")
                        null
                    }
                }

                Timber.d("Whitelist sync: Need to fetch ${artistsToFetch.size} of ${whitelistEntries.size} artists")

                // Process artists in parallel batches
                val batchSize = 300  // Process 300 artists concurrently (increased with optimized HTTP client)
                var successCount = 0
                var failureCount = 0
                var processedCount = 0

                artistsToFetch.chunked(batchSize).forEach { batch ->
                    supervisorScope {
                        val results = batch.map { (whitelistEntry, existingArtist) ->
                            async {
                                try {
                                    // Update progress
                                    processedCount++
                                    _whitelistSyncProgress.value = WhitelistSyncProgress(
                                        current = processedCount,
                                        total = artistsToFetch.size,
                                        currentArtistName = whitelistEntry.artistName
                                    )

                                    // Fetch artist details from YouTube
                                    YouTube.artist(whitelistEntry.artistId).onSuccess { artistPage ->
                                        database.transaction {
                                            if (existingArtist == null) {
                                                // Insert new artist with full metadata
                                                insert(
                                                    ArtistEntity(
                                                        id = whitelistEntry.artistId,
                                                        name = artistPage.artist.title,
                                                        thumbnailUrl = artistPage.artist.thumbnail,
                                                        channelId = artistPage.artist.channelId,
                                                        lastUpdateTime = LocalDateTime.now()
                                                    )
                                                )
                                                Timber.d("Whitelist sync: Inserted artist '${artistPage.artist.title}' with thumbnail")
                                            } else {
                                                // Update existing artist with fresh metadata
                                                update(
                                                    existingArtist.artist.copy(
                                                        name = artistPage.artist.title,
                                                        thumbnailUrl = artistPage.artist.thumbnail,
                                                        channelId = artistPage.artist.channelId,
                                                        lastUpdateTime = LocalDateTime.now()
                                                    )
                                                )
                                                Timber.d("Whitelist sync: Updated artist '${artistPage.artist.title}' with thumbnail")
                                            }
                                        }
                                        Result.success(Unit)
                                    }.onFailure { e ->
                                        Timber.w("Whitelist sync: Failed to fetch metadata for artist ${whitelistEntry.artistId}: ${e.message}")
                                        Result.failure<Unit>(e)
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "Whitelist sync: Exception fetching artist ${whitelistEntry.artistId}")
                                    Result.failure<Unit>(e)
                                }
                            }
                        }

                        // Await all results in this batch
                        val batchResults = results.awaitAll()
                        successCount += batchResults.count { it.isSuccess }
                        failureCount += batchResults.count { it.isFailure }
                    }
                }

                val skippedCount = whitelistEntries.size - artistsToFetch.size
                Timber.d("Whitelist sync: Fetched metadata for $successCount artists, $failureCount failures, $skippedCount skipped (already had thumbnails)")

                // Mark sync as complete
                _whitelistSyncProgress.value = WhitelistSyncProgress(
                    current = artistsToFetch.size,
                    total = artistsToFetch.size,
                    isComplete = true
                )

                    // Save sync timestamp and hash
                    context.dataStore.edit { settings ->
                        settings[LastWhitelistSyncTimeKey] = System.currentTimeMillis()
                        settings[LastWhitelistHashKey] = newHash
                    }
                    Timber.d("Whitelist sync: Saved sync timestamp and hash")

                } catch (e: Exception) {
                    Timber.e(e, "Whitelist sync exception: ${e.message}")
                    e.printStackTrace()
                } finally {
                    isSyncingWhitelist.value = false
                    Timber.d("Whitelist sync finished")
                }
            }.onFailure { e ->
                Timber.e(e, "Whitelist sync failed to fetch JSON: ${e.message}")
                e.printStackTrace()
                // Set complete to avoid stuck splash screen
                _whitelistSyncProgress.value = WhitelistSyncProgress(isComplete = true)
            }
        } else {
            // Force sync requested - use old method (fetch and sync regardless)
            Timber.d("Whitelist sync: Force sync requested")
            isSyncingWhitelist.value = true

            // Reset progress
            _whitelistSyncProgress.value = WhitelistSyncProgress()

            try {
                Timber.d("Whitelist sync starting...")
                WhitelistFetcher.fetchWhitelist().onSuccess { whitelistEntries ->
                    Timber.d("Whitelist sync: Fetched ${whitelistEntries.size} artists from GitHub")

                    // Update progress with total count
                    _whitelistSyncProgress.value = WhitelistSyncProgress(
                        total = whitelistEntries.size
                    )

                    // Get current whitelist IDs before clearing (for deletion comparison)
                    val currentWhitelistIds = database.getAllWhitelistedArtistIdsSync()
                    val newWhitelistIds = whitelistEntries.map { it.artistId }.toSet()
                    val removedArtistIds = currentWhitelistIds.filterNot { it in newWhitelistIds }

                    if (removedArtistIds.isNotEmpty()) {
                        Timber.d("Whitelist sync: Detected ${removedArtistIds.size} removed artists, deleting their content")
                        deleteRemovedArtists(removedArtistIds)
                    } else {
                        Timber.d("Whitelist sync: No artists removed from whitelist")
                    }

                    database.transaction {
                        clearWhitelist()
                        insertWhitelist(whitelistEntries)
                    }
                    Timber.d("Whitelist sync: Successfully synced ${whitelistEntries.size} artists to whitelist table")

                    // Update timestamp (no hash for force sync, will be updated on next normal sync)
                    context.dataStore.edit { settings ->
                        settings[LastWhitelistSyncTimeKey] = System.currentTimeMillis()
                    }

                    // Mark as complete immediately for force sync
                    _whitelistSyncProgress.value = WhitelistSyncProgress(isComplete = true)
                    Timber.d("Whitelist sync: Force sync complete")

                }.onFailure { e ->
                    Timber.e(e, "Whitelist sync failed: ${e.message}")
                    e.printStackTrace()
                    _whitelistSyncProgress.value = WhitelistSyncProgress(isComplete = true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Whitelist sync exception: ${e.message}")
                e.printStackTrace()
            } finally {
                isSyncingWhitelist.value = false
                Timber.d("Whitelist sync finished")
            }
        }
    }

    suspend fun clearAllSyncedContent() {
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes artists and all their associated content from the database.
     * This includes: songs, albums, play history, cached formats, lyrics, and user data.
     * Deletion follows proper order to respect foreign key constraints.
     */
    suspend fun deleteRemovedArtists(removedArtistIds: List<String>) {
        if (removedArtistIds.isEmpty()) return

        try {
            Timber.d("Artist deletion: Deleting ${removedArtistIds.size} removed artists and their content")

            // Process each removed artist
            for (artistId in removedArtistIds) {
                try {
                    Timber.d("Artist deletion: Processing artist $artistId")

                    // Step 1: Get all song IDs for this artist
                    val songIds = database.getSongIdsByArtist(artistId)
                    Timber.d("Artist deletion: Found ${songIds.size} songs for artist $artistId")

                    // Step 2: Get all album IDs for this artist
                    val albumIds = database.getAlbumIdsByArtist(artistId)
                    Timber.d("Artist deletion: Found ${albumIds.size} albums for artist $artistId")

                    // Step 3: Delete song-related data without foreign keys (must be done first)
                    if (songIds.isNotEmpty()) {
                        Timber.d("Artist deletion: Deleting play counts, formats, and lyrics for ${songIds.size} songs")
                        database.deletePlayCountBySongs(songIds)
                        database.deleteFormatBySongs(songIds)
                        database.deleteLyricsBySongs(songIds)
                    }

                    // Step 4: Delete songs (this will CASCADE DELETE to related tables)
                    // Cascades: song_artist_map, song_album_map, playlist_song_map, related_song_map, event
                    if (songIds.isNotEmpty()) {
                        Timber.d("Artist deletion: Deleting ${songIds.size} songs")
                        database.deleteSongsByIds(songIds)
                    }

                    // Step 5: Check and delete albums that have no songs left
                    val albumsToDelete = mutableListOf<String>()
                    for (albumId in albumIds) {
                        val remainingSongCount = database.getAlbumSongCount(albumId)
                        if (remainingSongCount == 0) {
                            albumsToDelete.add(albumId)
                        }
                    }
                    if (albumsToDelete.isNotEmpty()) {
                        Timber.d("Artist deletion: Deleting ${albumsToDelete.size} orphaned albums")
                        database.deleteAlbumsByIds(albumsToDelete)
                    }

                    // Step 6: Delete the artist (this will CASCADE DELETE remaining mappings)
                    Timber.d("Artist deletion: Deleting artist $artistId")
                    database.deleteArtistById(artistId)

                    Timber.d("Artist deletion: Successfully deleted artist $artistId and all associated content")
                } catch (e: Exception) {
                    Timber.e(e, "Artist deletion: Failed to delete artist $artistId")
                    e.printStackTrace()
                }
            }

            Timber.d("Artist deletion: Completed deletion of ${removedArtistIds.size} artists")
        } catch (e: Exception) {
            Timber.e(e, "Artist deletion: Exception during batch deletion")
            e.printStackTrace()
        }
    }
}
