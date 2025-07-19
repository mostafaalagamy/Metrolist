package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Timestamps(
    @SerialName("start")
    val start: Long? = null,
    @SerialName("end")
    val end: Long? = null,
)