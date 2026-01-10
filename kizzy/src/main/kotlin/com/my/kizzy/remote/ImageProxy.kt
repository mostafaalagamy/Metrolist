package com.my.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageProxyResponse(
    @SerialName("id")
    val id: String
)
