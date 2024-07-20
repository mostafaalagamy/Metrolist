package com.moxxaxx.innertube.models.body

import com.moxxaxx.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String?,
)
