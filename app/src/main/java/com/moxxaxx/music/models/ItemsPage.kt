package com.moxxaxx.music.models

import com.moxxaxx.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
