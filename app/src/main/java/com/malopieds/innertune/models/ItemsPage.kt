package com.malopieds.innertune.models

import com.malopieds.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
