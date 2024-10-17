package com.metrolist.music.models

import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
