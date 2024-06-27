package com.malopieds.innertube.models.body

import com.malopieds.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
