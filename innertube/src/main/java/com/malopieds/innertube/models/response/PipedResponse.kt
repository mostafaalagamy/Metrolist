package com.malopieds.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class PipedResponse(
    val audioStreams: List<AudioStream>,
) {
    @Serializable
    data class AudioStream(
        val itag: Int,
        val url: String,
        val bitrate: Int,
    )
}
