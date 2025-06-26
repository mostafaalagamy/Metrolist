package com.metrolist.music.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.metrolist.music.R
import com.metrolist.music.constants.MediaSessionConstants
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import kotlin.collections.distinctBy
import kotlin.collections.plus
import javax.inject.Inject

class MediaLibrarySessionCallback @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {

    private val TAG = MediaLibrarySessionCallback::class.simpleName.toString()
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    var toggleLike: () -> Unit = {}
    var toggleStartRadio: () -> Unit = {}
    var toggleLibrary: () -> Unit = {}

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands
                .buildUpon()
                .add(MediaSessionConstants.CommandToggleLike)
                .add(MediaSessionConstants.CommandToggleStartRadio)
                .add(MediaSessionConstants.CommandToggleLibrary)
                .add(MediaSessionConstants.CommandToggleShuffle)
                .add(MediaSessionConstants.CommandToggleRepeatMode)
                .build(),
            connectionResult.availablePlayerCommands,
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
            MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled
            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem.Builder()
                    .setMediaId(MusicService.ROOT)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    ).build(),
                params
            )
        )

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future(Dispatchers.IO) {
        LibraryResult.ofItemList(
            when (parentId) {
                MusicService.ROOT -> listOf(
                    browsableMediaItem(MusicService.SONG, context.getString(R.string.songs), null, drawableUri(R.drawable.music_note), MediaMetadata.MEDIA_TYPE_PLAYLIST),
                    browsableMediaItem(MusicService.ARTIST, context.getString(R.string.artists), null, drawableUri(R.drawable.artist), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                    browsableMediaItem(MusicService.ALBUM, context.getString(R.string.albums), null, drawableUri(R.drawable.album), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                    browsableMediaItem(MusicService.PLAYLIST, context.getString(R.string.playlists), null, drawableUri(R.drawable.queue_music), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
                )

                MusicService.SONG -> database.songsByCreateDateAsc().first().map { it.toMediaItem(parentId) }

                MusicService.ARTIST -> database.artistsByCreateDateAsc().first().map { artist ->
                    browsableMediaItem(
                        "${MusicService.ARTIST}/${artist.id}",
                        artist.artist.name,
                        context.resources.getQuantityString(R.plurals.n_song, artist.songCount, artist.songCount),
                        artist.artist.thumbnailUrl?.toUri(),
                        MediaMetadata.MEDIA_TYPE_ARTIST
                    )
                }

                MusicService.ALBUM -> database.albumsByCreateDateAsc().first().map { album ->
                    browsableMediaItem(
                        "${MusicService.ALBUM}/${album.id}",
                        album.album.title,
                        album.artists.joinToString { it.name },
                        album.album.thumbnailUrl?.toUri(),
                        MediaMetadata.MEDIA_TYPE_ALBUM
                    )
                }

                MusicService.PLAYLIST -> {
                    val likedCount = database.likedSongsCount().first()
                    val downloadCount = downloadUtil.downloads.value.size
                    listOf(
                        browsableMediaItem("${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}", context.getString(R.string.liked_songs), context.resources.getQuantityString(R.plurals.n_song, likedCount, likedCount), drawableUri(R.drawable.favorite), MediaMetadata.MEDIA_TYPE_PLAYLIST),
                        browsableMediaItem("${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}", context.getString(R.string.downloaded_songs), context.resources.getQuantityString(R.plurals.n_song, downloadCount, downloadCount), drawableUri(R.drawable.download), MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    ) + database.playlistsByCreateDateAsc().first().map { playlist ->
                        browsableMediaItem(
                            "${MusicService.PLAYLIST}/${playlist.id}",
                            playlist.playlist.name,
                            context.resources.getQuantityString(R.plurals.n_song, playlist.songCount, playlist.songCount),
                            playlist.thumbnails.firstOrNull()?.toUri(),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST
                        )
                    }
                }

                else -> when {
                    parentId.startsWith("${MusicService.ARTIST}/") ->
                        database.artistSongsByCreateDateAsc(parentId.removePrefix("${MusicService.ARTIST}/")).first().map {
                            it.toMediaItem(parentId)
                        }

                    parentId.startsWith("${MusicService.ALBUM}/") ->
                        database.albumSongs(parentId.removePrefix("${MusicService.ALBUM}/")).first().map {
                            it.toMediaItem(parentId)
                        }

                    parentId.startsWith("${MusicService.PLAYLIST}/") -> {
                        val playlistId = parentId.removePrefix("${MusicService.PLAYLIST}/")
                        val flow = when (playlistId) {
                            PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(SongSortType.CREATE_DATE, true)
                            PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                val downloads = downloadUtil.downloads.value
                                database.allSongs().map { songs ->
                                    songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                                        .map { it to downloads[it.id] }
                                        .sortedBy { it.second?.updateTimeMs ?: 0L }
                                        .map { it.first }
                                }
                            }

                            else -> database.playlistSongs(playlistId).map { list -> list.map { it.song } }
                        }
                        flow.first().map { it.toMediaItem(parentId) }
                    }

                    else -> emptyList()
                }
            },
            params
        )
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            database.song(mediaId).first()?.toMediaItem()?.let {
                LibraryResult.ofItem(it, null)
            } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
        }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        Log.d(TAG, "MediaLibrarySessionCallback.onSetMediaItems")
        val defaultResult = MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
        val path = mediaItems.firstOrNull()?.mediaId?.split("/") ?: return@future defaultResult
        Log.d(TAG, "Path: " + path.joinToString(";"))

        when (path.firstOrNull()) {
            MusicService.SONG -> {
                val songId = path.getOrNull(1) ?: return@future defaultResult
                val allSongs = database.songsByCreateDateAsc().first()
                MediaSession.MediaItemsWithStartPosition(
                    allSongs.map { it.toMediaItem() },
                    allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            MusicService.ARTIST -> {
                val songId = path.getOrNull(2) ?: return@future defaultResult
                val artistId = path.getOrNull(1) ?: return@future defaultResult
                val songs = database.artistSongsByCreateDateAsc(artistId).first()
                MediaSession.MediaItemsWithStartPosition(
                    songs.map { it.toMediaItem() },
                    songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            MusicService.ALBUM -> {
                val songId = path.getOrNull(2) ?: return@future defaultResult
                val albumId = path.getOrNull(1) ?: return@future defaultResult
                val album = database.albumWithSongs(albumId).first() ?: return@future defaultResult
                MediaSession.MediaItemsWithStartPosition(
                    album.songs.map { it.toMediaItem() },
                    album.songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            MusicService.PLAYLIST -> {
                val songId = path.getOrNull(2) ?: return@future defaultResult
                val playlistId = path.getOrNull(1) ?: return@future defaultResult
                val songs = when (playlistId) {
                    PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(SongSortType.CREATE_DATE, true)
                    PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                        val downloads = downloadUtil.downloads.value
                        database.allSongs().map { songs ->
                            songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                                .map { it to downloads[it.id] }
                                .sortedBy { it.second?.updateTimeMs ?: 0L }
                                .map { it.first }
                        }
                    }

                    else -> database.playlistSongs(playlistId).map { it.map { it.song } }
                }.first()
                MediaSession.MediaItemsWithStartPosition(
                    songs.map { it.toMediaItem() },
                    songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            MusicService.SEARCH -> {
                val songId = path.getOrNull(2) ?: return@future defaultResult
                val searchQuery = path.getOrNull(1) ?: return@future defaultResult

                val results = combine<List<Song>, List<Song>, List<Song>>(
                    database.searchSongs(searchQuery),
                    database.searchArtists(searchQuery),
                ) { songs, artistSongs ->
                    (songs + artistSongs).distinctBy { it.id }
                }

                val songs = results.first()
                MediaSession.MediaItemsWithStartPosition(
                    songs.map { it.toMediaItem() },
                    songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            else -> defaultResult
        }
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        Log.d(TAG, "MediaLibrarySessionCallback.onSearch: $query")
        session.notifySearchResultChanged(browser, query, 1, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
        if (query.isEmpty()) {
            return@future LibraryResult.ofItemList(emptyList(), params)
        }

        try {
            val results = combine<List<Song>, List<Song>, List<Song>>(
                database.searchSongs(query),
                database.searchArtists(query),
            ) { songs, artistSongs ->
                (songs + artistSongs).distinctBy { it.id }
            }

            val items = results.first().map {
                it.toMediaItem("${MusicService.SEARCH}/$query")
            }
            LibraryResult.ofItemList(items, params)
        } catch (e: Exception) {
            Log.d(TAG, "Could not get search results")
            reportException(e)
            LibraryResult.ofItemList(emptyList(), params)
        }
    }

    private fun drawableUri(@DrawableRes id: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .build()
        ).build()

    private fun Song.toMediaItem(path: String): MediaItem = MediaItem.Builder()
        .setMediaId("$path/$id")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        ).build()

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        ).build()
}
