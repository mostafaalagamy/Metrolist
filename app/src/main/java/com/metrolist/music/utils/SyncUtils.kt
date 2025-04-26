package com.metrolist.music.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.*
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.*
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.*
import com.metrolist.music.extensions.isInternetConnected
import com.metrolist.music.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val TAG = "SyncUtils"
    private val syncCooldownSeconds = 30 * 60L

    private val _isSyncingLikedSongs = MutableStateFlow(false)
    private val _isSyncingLibrarySongs = MutableStateFlow(false)
    private val _isSyncingLikedAlbums = MutableStateFlow(false)
    private val _isSyncingArtists = MutableStateFlow(false)
    private val _isSyncingPlaylists = MutableStateFlow(false)

    val isSyncingLikedSongs: StateFlow<Boolean> = _isSyncingLikedSongs.asStateFlow()
    val isSyncingLibrarySongs: StateFlow<Boolean> = _isSyncingLibrarySongs.asStateFlow()
    val isSyncingLikedAlbums: StateFlow<Boolean> = _isSyncingLikedAlbums.asStateFlow()
    val isSyncingArtists: StateFlow<Boolean> = _isSyncingArtists.asStateFlow()
    val isSyncingPlaylists: StateFlow<Boolean> = _isSyncingPlaylists.asStateFlow()

    private fun canSync(lastSyncKey: Preferences.Key<Long>): Boolean {
        val lastSync = context.dataStore.get(lastSyncKey, 0L)
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        return (now - lastSync) >= syncCooldownSeconds
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun likeSong(s: SongEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs(bypass: Boolean = false) {
        if (_isSyncingLikedSongs.value) return
        if (!context.isInternetConnected()) return
        if (!bypass && !canSync(LastLikeSongSyncKey)) return

        _isSyncingLikedSongs.value = true
        try {
            YouTube.playlist("LM").completed().onSuccess { page ->
                val remoteSongs = page.songs.reversed()
                val localSongs = database.likedSongsByNameAsc().first()

                val remoteIds = remoteSongs.map { it.id }
                val localIds = localSongs.map { it.song.id }

                coroutineScope {
                    remoteSongs.forEach { remoteSong ->
                        launch(Dispatchers.IO) {
                            val localSong = database.song(remoteSong.id).firstOrNull()
                            if (localSong == null) {
                                database.insert(remoteSong.toMediaMetadata().toSongEntity().copy(
                                    liked = true,
                                    likedDate = LocalDateTime.now()
                                ))
                            } else if (!localSong.song.liked) {
                                database.update(localSong.song.localToggleLike())
                            }
                        }
                    }

                    localSongs.filter { it.song.id !in remoteIds }
                        .forEach { song ->
                            launch(Dispatchers.IO) {
                                database.update(song.song.localToggleLike())
                            }
                        }
                }
            }
            context.dataStore.edit { it[LastLikeSongSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        } finally {
            _isSyncingLikedSongs.value = false
        }
    }

    suspend fun syncLibrarySongs(bypass: Boolean = false) {
        if (_isSyncingLibrarySongs.value) return
        if (!context.isInternetConnected()) return
        if (!bypass && !canSync(LastLibSongSyncKey)) return

        _isSyncingLibrarySongs.value = true
        try {
            YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>()
                val localSongs = database.songsByNameAsc().first()

                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localIds = localSongs.map { it.song.id }.toSet()

                coroutineScope {
                    remoteSongs.forEach { song ->
                        launch(Dispatchers.IO) {
                            val dbSong = database.song(song.id).firstOrNull()
                            if (dbSong == null) {
                                database.insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                            } else if (dbSong.song.inLibrary == null) {
                                database.update(dbSong.song.toggleLibrary())
                            }
                        }
                    }

                    localSongs.filter { it.song.id !in remoteIds }
                        .forEach { song ->
                            launch(Dispatchers.IO) {
                                database.update(song.song.toggleLibrary())
                            }
                        }
                }
            }
            context.dataStore.edit { it[LastLibSongSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        } finally {
            _isSyncingLibrarySongs.value = false
        }
    }

    suspend fun syncLikedAlbums(bypass: Boolean = false) {
        if (_isSyncingLikedAlbums.value) return
        if (!context.isInternetConnected()) return
        if (!bypass && !canSync(LastAlbumSyncKey)) return

        _isSyncingLikedAlbums.value = true
        try {
            YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>()
                val localAlbums = database.albumsLikedByNameAsc().first()

                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localIds = localAlbums.map { it.id }.toSet()

                coroutineScope {
                    remoteAlbums.forEach { album ->
                        launch(Dispatchers.IO) {
                            val dbAlbum = database.album(album.id).firstOrNull()
                            if (dbAlbum == null || dbAlbum.album.bookmarkedAt == null) {
                                YouTube.album(album.browseId).onSuccess { albumPage ->
                                    database.insert(albumPage)
                                    database.album(album.id).firstOrNull()?.let {
                                        database.update(it.album.localToggleLike())
                                    }
                                }
                            }
                        }
                    }

                    localAlbums.filter { it.id !in remoteIds }
                        .forEach { album ->
                            launch(Dispatchers.IO) {
                                database.update(album.album.localToggleLike())
                            }
                        }
                }
            }
            context.dataStore.edit { it[LastAlbumSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        } finally {
            _isSyncingLikedAlbums.value = false
        }
    }

    suspend fun syncArtistsSubscriptions(bypass: Boolean = false) {
        if (_isSyncingArtists.value) return
        if (!context.isInternetConnected()) return
        if (!bypass && !canSync(LastArtistSyncKey)) return

        _isSyncingArtists.value = true
        try {
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
                val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                val localArtists = database.artistsBookmarkedByNameAsc().first()

                val remoteIds = remoteArtists.map { it.id }.toSet()
                val localIds = localArtists.map { it.id }.toSet()

                coroutineScope {
                    remoteArtists.forEach { artist ->
                        launch(Dispatchers.IO) {
                            val dbArtist = database.artist(artist.id).firstOrNull()
                            if (dbArtist == null || dbArtist.artist.bookmarkedAt == null) {
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
                        }
                    }

                    localArtists.filter { it.id !in remoteIds }
                        .forEach { artist ->
                            launch(Dispatchers.IO) {
                                database.update(artist.artist.localToggleLike())
                            }
                        }
                }
            }
            context.dataStore.edit { it[LastArtistSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        } finally {
            _isSyncingArtists.value = false
        }
    }

    suspend fun syncSavedPlaylists(bypass: Boolean = false) {
        if (_isSyncingPlaylists.value) return
        if (!context.isInternetConnected()) return
        if (!bypass && !canSync(LastPlaylistSyncKey)) return

        _isSyncingPlaylists.value = true
        try {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "LM" || it.id == "SE" }
                val localPlaylists = database.playlistsByNameAsc().first()

                val remoteIds = remotePlaylists.map { it.id }.toSet()
                val localIds = localPlaylists.mapNotNull { it.playlist.browseId }.toSet()

                coroutineScope {
                    remotePlaylists.forEach { playlist ->
                        launch(Dispatchers.IO) {
                            val localPlaylist = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                            if (localPlaylist == null) {
                                val newPlaylist = PlaylistEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    isEditable = playlist.isEditable,
                                    bookmarkedAt = LocalDateTime.now(),
                                    remoteSongCount = playlist.songCountText?.toIntOrNull(),
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                )
                                database.insert(newPlaylist)

                                syncPlaylistContent(playlist.id, newPlaylist.id)
                            }
                        }
                    }

                    localPlaylists.filter { it.playlist.browseId !in remoteIds }
                        .forEach { playlist ->
                            launch(Dispatchers.IO) {
                                database.update(playlist.playlist.localToggleLike())
                            }
                        }
                }
            }
            context.dataStore.edit { it[LastPlaylistSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) }
        } finally {
            _isSyncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylistContent(browseId: String, playlistId: String) {
        YouTube.playlist(browseId).completed().onSuccess { page ->
            coroutineScope {
                val songEntities = page.songs.map(SongItem::toMediaMetadata)
                database.transaction {
                    songEntities.forEach { database.insert(it) }
                    songEntities.forEachIndexed { index, song ->
                        database.insert(
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistId,
                                position = index,
                                setVideoId = song.setVideoId
                            )
                        )
                    }
                }
            }
        }
    }
}
