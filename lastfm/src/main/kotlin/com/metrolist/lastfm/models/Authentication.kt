package com.metrolist.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class Authentication(
    val session: Session
) {
    @Serializable
    data class Session(
        val name: String,       // Username
        val key: String,        // Session Key
        val subscriber: Int,    // Last.fm Pro?
    )
}
