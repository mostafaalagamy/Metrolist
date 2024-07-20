package com.moxxaxx.innertube.models.body

import com.moxxaxx.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
)
