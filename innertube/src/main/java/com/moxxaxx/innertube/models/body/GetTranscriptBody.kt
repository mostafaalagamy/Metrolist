package com.moxxaxx.innertube.models.body

import com.moxxaxx.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
