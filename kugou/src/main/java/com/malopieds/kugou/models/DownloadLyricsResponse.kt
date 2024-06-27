package com.malopieds.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val content: String,
)
