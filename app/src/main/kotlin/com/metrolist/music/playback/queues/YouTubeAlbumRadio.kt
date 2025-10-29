package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.filterWhitelisted
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeAlbumRadio(
    private var playlistId: String,
    private val database: MusicDatabase,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId,
            params = "wAEB"
        )

    private var albumSongCount = 0
    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
        albumSongCount = albumSongs.size

        // Filter by whitelist before converting to MediaItems
        val filteredSongs = albumSongs.filterWhitelisted(database).filterIsInstance<SongItem>()

        Queue.Status(
            title = albumSongs.firstOrNull()?.album?.name.orEmpty(),
            items = filteredSongs.map { it.toMediaItem() },
            mediaItemIndex = 0
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
        continuation = nextResult.continuation

        // Filter by whitelist before converting to MediaItems
        val filteredItems = if (!firstTimeLoaded) {
            firstTimeLoaded = true
            nextResult.items.subList(albumSongCount, nextResult.items.size).filterWhitelisted(database).filterIsInstance<SongItem>()
        } else {
            nextResult.items.filterWhitelisted(database).filterIsInstance<SongItem>()
        }

        filteredItems.map { it.toMediaItem() }
    }
}
