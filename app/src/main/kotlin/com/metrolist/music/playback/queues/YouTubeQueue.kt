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

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
    private val database: MusicDatabase,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation

        // Filter by whitelist before converting to MediaItems
        val filteredItems = nextResult.items.filterWhitelisted(database).filterIsInstance<SongItem>()

        return Queue.Status(
            title = nextResult.title,
            items = filteredItems.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation

        // Filter by whitelist before converting to MediaItems
        val filteredItems = nextResult.items.filterWhitelisted(database).filterIsInstance<SongItem>()

        return filteredItems.map { it.toMediaItem() }
    }

    companion object {
        fun radio(song: MediaMetadata, database: MusicDatabase) = YouTubeQueue(WatchEndpoint(song.id), song, database)
    }
}
