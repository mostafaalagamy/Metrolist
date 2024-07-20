package com.moxxaxx.innertube.models.body

import com.moxxaxx.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
