package com.my.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Identify(
    @SerialName("capabilities")
    val capabilities: Int,
    @SerialName("compress")
    val compress: Boolean,
    @SerialName("largeThreshold")
    val largeThreshold: Int,
    @SerialName("properties")
    val properties: Properties,
    @SerialName("token")
    val token: String,
) {
    companion object {
        fun String.toIdentifyPayload() = Identify(
            capabilities = 65,
            compress = false,
            largeThreshold = 100,
            properties = Properties(
                browser = "Discord Client",
                device = "ktor",
                os = "Windows"
            ),
            token = this
        )
    }
}

@Serializable
data class Properties(
    @SerialName("browser")
    val browser: String,
    @SerialName("device")
    val device: String,
    @SerialName("os")
    val os: String,
)