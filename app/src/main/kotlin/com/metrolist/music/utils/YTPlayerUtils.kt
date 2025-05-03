package com.metrolist.music.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.metrolist.innertube.NewPipeUtils
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.YouTubeClient.Companion.IOS
import com.metrolist.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.metrolist.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.music.constants.AudioQuality
import okhttp3.OkHttpClient
import okhttp3.Request

object YTPlayerUtils {
    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        YouTubeClient.WEB,
        YouTubeClient.MOBILE
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()

        val isLoggedIn = YouTube.cookie != null

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()

        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        val allClients = listOf(MAIN_CLIENT) + STREAM_FALLBACK_CLIENTS

        for (client in allClients) {
            if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) continue

            streamPlayerResponse = if (client == MAIN_CLIENT) {
                mainPlayerResponse
            } else {
                YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            if (streamPlayerResponse?.playabilityStatus?.status != "OK") continue

            val audioFormats = streamPlayerResponse.streamingData?.adaptiveFormats?.filter { it.isAudio } ?: continue
            val bestFormat = audioFormats.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
            } ?: continue

            streamUrl = NewPipeUtils.getStreamUrl(bestFormat, videoId).getOrNull()
            if (streamUrl == null || !validateStatus(streamUrl)) continue

            format = bestFormat
            streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 3600
            break
        }

        if (streamPlayerResponse == null || streamUrl == null || format == null) {
            throw PlaybackException("Failed to get working stream", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }

        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds!!
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null
    ): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = WEB_REMIX)

    private fun validateStatus(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .head()
                .url(url)
                .build()
            val response = httpClient.newCall(request).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
