package com.malopieds.innertune.playback.queues

import androidx.media3.common.MediaItem
import com.malopieds.innertune.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?
    suspend fun getInitialStatus(): Status
    fun hasNextPage(): Boolean
    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    )
}
