package com.my.kizzy.data.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    @SerialName("large_image")
    val largeImage: String?,
    @SerialName("small_image")
    val smallImage: String?,
    @SerialName("large_text")
    val largeText: String? = null,
    @SerialName("small_text")
    val smallText: String? = null,
)