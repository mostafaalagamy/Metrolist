package com.metrolist.music.models

import com.metrolist.music.db.entities.LocalItem
import com.metrolist.innertube.models.YTItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
