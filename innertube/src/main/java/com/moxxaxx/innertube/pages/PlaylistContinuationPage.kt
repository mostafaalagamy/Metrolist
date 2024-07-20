package com.moxxaxx.innertube.pages

import com.moxxaxx.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
