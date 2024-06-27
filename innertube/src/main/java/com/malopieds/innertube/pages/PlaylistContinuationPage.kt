package com.malopieds.innertube.pages

import com.malopieds.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
