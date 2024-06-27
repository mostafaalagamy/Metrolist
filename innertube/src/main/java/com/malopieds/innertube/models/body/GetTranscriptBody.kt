package com.malopieds.innertube.models.body

import com.malopieds.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
