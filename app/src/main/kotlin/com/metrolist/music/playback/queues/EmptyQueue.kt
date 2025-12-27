/*
 * Copyright (C) 2026 Metrolist Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.models.MediaMetadata

object EmptyQueue : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus() = Queue.Status(null, emptyList(), -1)

    override fun hasNextPage() = false

    override suspend fun nextPage() = emptyList<MediaItem>()
}
