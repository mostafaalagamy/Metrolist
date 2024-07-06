package com.malopieds.innertune.lyrics

import android.content.Context
import com.malopieds.innertube.YouTube
import com.malopieds.innertube.models.WatchEndpoint

object YouTubeLyricsProvider : LyricsProvider {
    override val name = "YouTube Music"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> =
        runCatching {
            val nextResult = YouTube.next(WatchEndpoint(videoId = id)).getOrThrow()
            YouTube
                .lyrics(
                    endpoint = nextResult.lyricsEndpoint ?: throw IllegalStateException("Lyrics endpoint not found"),
                ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
        }
}
