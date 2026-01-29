package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
    private val request: Request = Request(),
    private val user: User = User()
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val osName: String? = null,
        val osVersion: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: String? = null,
        val gl: String,
        val hl: String,
        val visitorData: String?,
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String,
    )

    @Serializable
    data class Request(
        val internalExperimentFlags: Array<String> = emptyArray(),
        val useSsl: Boolean = true,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Request

            if (useSsl != other.useSsl) return false
            if (!internalExperimentFlags.contentEquals(other.internalExperimentFlags)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = useSsl.hashCode()
            result = 31 * result + internalExperimentFlags.contentHashCode()
            return result
        }
    }

    @Serializable
    data class User(
        val lockedSafetyMode: Boolean = false,
        val onBehalfOfUser: String? = null,
    )
}
