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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs() = coroutineScope {
        YouTube.playlist("LM").completed().onSuccess { page ->
            val remoteSongs = page.songs
            val remoteIds = remoteSongs.map { it.id }
            val localSongs = database.likedSongsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.localToggleLike()) }

            val now = LocalDateTime.now()
            remoteSongs.forEachIndexed { index, song ->
                launch {
                    val dbSong = database.song(song.id).firstOrNull()
                    val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                    if (dbSong == null) {
                        database.insert(song.toMediaMetadata().toSongEntity().copy(liked = true, likedDate = timestamp))
                    } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                        database.update(dbSong.song.copy(liked = true, likedDate = timestamp))
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
            val remoteIds = remoteSongs.map { it.id }.toSet()
            val localSongs = database.songsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.toggleLibrary()) }

            remoteSongs.forEach { song ->
                launch {
                    val dbSong = database.song(song.id).firstOrNull()
                    if (dbSong == null) {
                        database.insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                    } else if (dbSong.song.inLibrary == null) {
                        database.update(dbSong.song.toggleLibrary())
                    }
                }
            }
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
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let { Regex("""\\d+""").find(it)?.value?.toIntOrNull() },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                    }
                    syncPlaylist(playlist.id, playlistEntity.id)
                }
            }
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        YouTube.playlist(browseId).completed().onSuccess { page ->
            val songs = page.songs.map(SongItem::toMediaMetadata)

            val remoteIds = songs.map { it.id }
            val localIds = database.playlistSongs(playlistId).first()
                .sortedBy { it.map.position }
                .map { it.song.id }

            if (remoteIds == localIds) return@onSuccess

            database.transaction {
                runBlocking {
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
            }
        }
    }
}
