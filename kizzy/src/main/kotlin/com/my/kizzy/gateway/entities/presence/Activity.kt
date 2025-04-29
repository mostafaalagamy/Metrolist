package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    @SerialName("name")
    val name: String?,
    @SerialName("state")
    val state: String? = null,
    @SerialName("details")
    val details: String? = null,
    @SerialName("type")
    val type: Int? = 0,
    @SerialName("timestamps")
    val timestamps: Timestamps? = null,
    @SerialName("assets")
    val assets: Assets? = null,
    @SerialName("buttons")
    val buttons: List<String?>? = null,
    @SerialName("metadata")
    val metadata: Metadata? = null,
    @SerialName("application_id")
    val applicationId: String? = null,
    @SerialName("url")
    val url: String? = null,
)
