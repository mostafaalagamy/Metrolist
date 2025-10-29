package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.filterWhitelisted
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class LocalAlbumRadio(
    private val albumWithSongs: AlbumWithSongs,
    private val startIndex: Int = 0,
    private val database: MusicDatabase,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private lateinit var playlistId: String
    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId,
            params = "wAEB"
        )

    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        Queue.Status(
            title = albumWithSongs.album.title,
            items = albumWithSongs.songs.map { it.toMediaItem() },
            mediaItemIndex = startIndex
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        if (!firstTimeLoaded) {
            playlistId = YouTube.album(albumWithSongs.album.id).getOrThrow().album.playlistId
            val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
            continuation = nextResult.continuation
            firstTimeLoaded = true

            // Filter by whitelist before converting to MediaItems
            val filteredItems = nextResult.items.subList(
                albumWithSongs.songs.size,
                nextResult.items.size
            ).filterWhitelisted(database).filterIsInstance<SongItem>()

            return@withContext filteredItems.map { it.toMediaItem() }
        }
        val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
        continuation = nextResult.continuation

        // Filter by whitelist before converting to MediaItems
        val filteredItems = nextResult.items.filterWhitelisted(database).filterIsInstance<SongItem>()

        filteredItems.map { it.toMediaItem() }
    }
}
