package com.metrolist.simpmusic.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsData(
    val id: String? = null,
    val videoId: String? = null,
    @SerialName("songTitle")
    val title: String? = null,
    @SerialName("artistName")
    val artist: String? = null,
    @SerialName("albumName")
    val album: String? = null,
    @SerialName("durationSeconds")
    val duration: Int? = null,
    val syncedLyrics: String? = null,
    @SerialName("plainLyric")
    val plainLyrics: String? = null,
    val richSyncLyrics: String? = null,
    val vote: Int? = null,
)

@Serializable
data class SimpMusicApiResponse(
    val type: String? = null,
    val data: List<LyricsData> = emptyList(),
) {
    val success: Boolean
        get() = type == "success"
}
