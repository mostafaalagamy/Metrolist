package com.malopieds.innertune.playback.queues

import androidx.media3.common.MediaItem
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.WatchEndpoint
import com.malopieds.innertune.extensions.toMediaItem
import com.malopieds.innertune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
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
        return nextResult.items.map { it.toMediaItem() }
    }
}
