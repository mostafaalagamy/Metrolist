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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
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
            val localSongs = database.likedSongsByNameAsc().first()

            val remoteIds = remoteSongs.map { it.id }
            val localIds = localSongs.map { it.song.id }

            if (remoteIds != localIds) {
                // Unlike removed songs
                localSongs.filter { it.song.id !in remoteIds }.map {
                    async { database.update(it.song.localToggleLike()) }
                }.awaitAll()

                // Like new songs
                remoteSongs.map { song ->
                    async {
                        val dbSong = database.song(song.id).firstOrNull()
                        if (dbSong == null) {
                            database.insert(
                                song.toMediaMetadata().toSongEntity().copy(
                                    liked = true,
                                    likedDate = LocalDateTime.now()
                                )
                            )
                        } else if (!dbSong.song.liked) {
                            database.update(dbSong.song.localToggleLike())
                        }
                    }
                }.awaitAll()
            } else {
                Log.d(TAG, "Liked songs are up to date, skipping update.")
            }
        }.onFailure {
            Log.e(TAG, "Failed to sync liked songs", it)
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        YouTube.library("FEmusic_liked_videos").completedLibraryPage().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>()
            val localSongs = database.songsByNameAsc().first()

            val remoteIds = remoteSongs.map { it.id }
            val localIds = localSongs.map { it.song.id }

            if (remoteIds != localIds) {
                // Remove songs no longer in library
                localSongs.filter { it.song.id !in remoteIds }.map {
                    async { database.update(it.song.toggleLibrary()) }
                }.awaitAll()

                // Add new songs to library
                remoteSongs.map { song ->
                    async {
                        val dbSong = database.song(song.id).firstOrNull()
                        if (dbSong == null) {
                            database.insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                        } else if (dbSong.song.inLibrary == null) {
                            database.update(dbSong.song.toggleLibrary())
                        }
                    }
                }.awaitAll()
            } else {
                Log.d(TAG, "Library songs are up to date, skipping update.")
            }
        }
    }

    suspend fun syncLikedAlbums() = coroutineScope {
        YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess { page ->
            val remoteAlbums = page.items.filterIsInstance<AlbumItem>()
            val localAlbums = database.albumsLikedByNameAsc().first()

            val remoteIds = remoteAlbums.map { it.id }
            val localIds = localAlbums.map { it.id }

            if (remoteIds != localIds) {
                // Remove unliked albums
                localAlbums.filter { it.id !in remoteIds }.map {
                    async { database.update(it.album.localToggleLike()) }
                }.awaitAll()

                // Like new albums
                remoteAlbums.map { album ->
                    async {
                        val dbAlbum = database.album(album.id).firstOrNull()
                        YouTube.album(album.browseId).onSuccess { albumPage ->
                            if (dbAlbum == null) {
                                database.insert(albumPage)
                                database.album(album.id).firstOrNull()?.let {
                                    database.update(it.album.localToggleLike())
                                }
                            } else if (dbAlbum.album.bookmarkedAt == null) {
                                database.update(dbAlbum.album.localToggleLike())
                            }
                        }
                    }
                }.awaitAll()
            } else {
                Log.d(TAG, "Albums are up to date, skipping update.")
            }
        }
    }

    suspend fun syncArtistsSubscriptions() = coroutineScope {
        YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess { page ->
            val remoteArtists = page.items.filterIsInstance<ArtistItem>()
            val localArtists = database.artistsBookmarkedByNameAsc().first()

            val remoteIds = remoteArtists.map { it.id }
            val localIds = localArtists.map { it.id }

            if (remoteIds != localIds) {
                // Remove unsubscribed artists
                localArtists.filter { it.id !in remoteIds }.map {
                    async { database.update(it.artist.localToggleLike()) }
                }.awaitAll()

                // Subscribe to new artists
                remoteArtists.map { artist ->
                    async {
                        val dbArtist = database.artist(artist.id).firstOrNull()
                        if (dbArtist == null) {
                            database.insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        } else if (dbArtist.artist.bookmarkedAt == null) {
                            database.update(dbArtist.artist.localToggleLike())
                        }
                    }
                }.awaitAll()
            } else {
                Log.d(TAG, "Artists are up to date, skipping update.")
            }
        }
    }

    suspend fun syncSavedPlaylists() = coroutineScope {
        YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess { page ->
            val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" }
            val localPlaylists = database.playlistsByNameAsc().first()

            val remoteIds = remotePlaylists.map { it.id }
            val localIds = localPlaylists.mapNotNull { it.playlist.browseId }

            if (remoteIds != localIds) {
                // Remove unsaved playlists
                localPlaylists.filter { it.playlist.browseId !in remoteIds }.map {
                    async {
                        if (it.playlist.bookmarkedAt != null) {
                            database.update(it.playlist.localToggleLike())
                        }
                    }
                }.awaitAll()

                // Add or update saved playlists
                remotePlaylists.map { remote ->
                    async {
                        val existing = localPlaylists.find { it.playlist.browseId == remote.id }?.playlist
                        if (existing == null) {
                            val newPlaylist = PlaylistEntity(
                                id = UUID.randomUUID().toString(),
                                name = remote.title,
                                browseId = remote.id,
                                isEditable = remote.isEditable,
                                bookmarkedAt = LocalDateTime.now(),
                                remoteSongCount = remote.songCountText?.toIntOrNull(),
                                playEndpointParams = remote.playEndpoint?.params,
                                shuffleEndpointParams = remote.shuffleEndpoint?.params,
                                radioEndpointParams = remote.radioEndpoint?.params
                            )
                            database.insert(newPlaylist)
                            syncPlaylist(remote.id, newPlaylist.id)
                        } else {
                            if (existing.name != remote.title || existing.remoteSongCount != remote.songCountText?.toIntOrNull()) {
                                database.update(
                                    existing.copy(
                                        name = remote.title,
                                        remoteSongCount = remote.songCountText?.toIntOrNull()
                                    )
                                )
                            }
                            if (existing.isEditable) {
                                syncPlaylist(remote.id, existing.id)
                            }
                        }
                    }
                }.awaitAll()
            } else {
                Log.d(TAG, "Playlists are up to date, skipping update.")
            }
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        YouTube.playlist(browseId).completed().onSuccess { playlistPage ->
            val remoteSongs = playlistPage.songs.map { it.id }
            val currentSongs = database.playlistSongs(playlistId).first().sortedBy { it.map.position }
            val localSongs = currentSongs.map { it.song.id }

            if (remoteSongs != localSongs) {
                launch(Dispatchers.IO) {
                    database.transaction {
                        runBlocking {
                            database.clearPlaylist(playlistId)

                            playlistPage.songs.forEachIndexed { index, songItem ->
                                val song = songItem.toMediaMetadata()
                                if (database.song(song.id).firstOrNull() == null) {
                                    database.insert(song)
                                }
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
            } else {
                Log.d(TAG, "Playlist $browseId is up to date, skipping playlist songs update.")
            }
        }.onFailure {
            Log.e(TAG, "Failed to sync playlist $browseId", it)
        }
    }
}
