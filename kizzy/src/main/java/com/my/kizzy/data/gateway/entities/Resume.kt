package com.my.kizzy.data.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resume(
    @SerialName("seq")
    val seq: Int,
    @SerialName("session_id")
    val sessionId: String?,
    @SerialName("token")
    val token: String,
)