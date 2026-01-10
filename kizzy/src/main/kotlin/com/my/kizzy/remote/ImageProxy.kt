package com.my.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageProxyResponse(
    val results: List<ImageProxyResult>
)

@Serializable
data class ImageProxyResult(
    @SerialName("original_url")
    val originalUrl: String,
    val status: String,
    val id: String
)
