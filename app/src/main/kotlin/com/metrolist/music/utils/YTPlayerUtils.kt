/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.metrolist.music.constants.AudioQuality
import com.metrolist.innertube.NewPipeUtils
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.metrolist.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.metrolist.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.metrolist.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.metrolist.innertube.models.YouTubeClient.Companion.IOS
import com.metrolist.innertube.models.YouTubeClient.Companion.IPADOS
import com.metrolist.innertube.models.YouTubeClient.Companion.MOBILE
import com.metrolist.innertube.models.YouTubeClient.Companion.TVHTML5
import com.metrolist.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.metrolist.innertube.models.YouTubeClient.Companion.WEB
import com.metrolist.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.metrolist.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.metrolist.innertube.models.response.PlayerResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    
    // Timeout for parallel client requests (ms) - reduced for faster playback start
    private const val PARALLEL_REQUEST_TIMEOUT_MS = 3000L
    
    // Number of clients to try in parallel for fallback - increased for faster results
    private const val PARALLEL_BATCH_SIZE = 4

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()
    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.metrolist.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     * Ordered by reliability/speed.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_1_61_48,
        ANDROID_VR_1_43_32,
        ANDROID_CREATOR,
        IPADOS,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        TVHTML5,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    
    /**
     * Data class to hold client stream result
     */
    private data class ClientStreamResult(
        val client: YouTubeClient,
        val playerResponse: PlayerResponse?,
        val format: PlayerResponse.StreamingData.Format?,
        val streamUrl: String?,
        val expiresInSeconds: Int?
    )
    
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        // Get main client response for metadata
        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking

        // Try main client first
        val mainResult = tryGetStreamFromResponse(
            MAIN_CLIENT,
            mainPlayerResponse,
            videoId,
            audioQuality,
            connectivityManager
        )
        
        if (mainResult != null && mainResult.streamUrl != null && mainResult.format != null && mainResult.expiresInSeconds != null) {
            Timber.tag(logTag).d("Successfully got stream from MAIN_CLIENT")
            return@runCatching PlaybackData(
                audioConfig,
                videoDetails,
                playbackTracking,
                mainResult.format,
                mainResult.streamUrl,
                mainResult.expiresInSeconds,
            )
        }
        
        Timber.tag(logTag).d("MAIN_CLIENT failed, trying fallback clients in parallel")
        
        // Filter clients that can be used
        val availableClients = STREAM_FALLBACK_CLIENTS.filter { client ->
            !(client.loginRequired && !isLoggedIn && YouTube.cookie == null)
        }
        
        // Try fallback clients in parallel batches
        var result: ClientStreamResult? = null
        
        for (batchStart in availableClients.indices step PARALLEL_BATCH_SIZE) {
            val batchEnd = minOf(batchStart + PARALLEL_BATCH_SIZE, availableClients.size)
            val batch = availableClients.subList(batchStart, batchEnd)
            
            Timber.tag(logTag).d("Trying fallback batch ${batchStart / PARALLEL_BATCH_SIZE + 1}: ${batch.map { it.clientName }}")
            
            result = tryClientsInParallel(
                batch,
                videoId,
                playlistId,
                signatureTimestamp,
                audioQuality,
                connectivityManager
            )
            
            if (result != null) {
                Timber.tag(logTag).d("Found working stream from client: ${result.client.clientName}")
                break
            }
        }
        
        // Validate final result
        if (result == null || result.playerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (result.playerResponse.playabilityStatus.status != "OK") {
            val errorReason = result.playerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (result.expiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (result.format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (result.streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${result.format.mimeType}, bitrate: ${result.format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            result.format,
            result.streamUrl,
            result.expiresInSeconds,
        )
    }
    
    /**
     * Try multiple clients in parallel and return the first successful result.
     */
    private suspend fun tryClientsInParallel(
        clients: List<YouTubeClient>,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager
    ): ClientStreamResult? = coroutineScope {
        val results = clients.map { client ->
            async {
                try {
                    withTimeoutOrNull(PARALLEL_REQUEST_TIMEOUT_MS) {
                        Timber.tag(logTag).d("Parallel request for client: ${client.clientName}")
                        val response = YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
                        if (response != null) {
                            tryGetStreamFromResponse(client, response, videoId, audioQuality, connectivityManager)
                        } else null
                    }
                } catch (e: Exception) {
                    Timber.tag(logTag).w(e, "Parallel request failed for client: ${client.clientName}")
                    null
                }
            }
        }
        
        // Wait for all and return first valid result
        results.awaitAll().filterNotNull().firstOrNull { 
            it.streamUrl != null && it.format != null && it.expiresInSeconds != null 
        }
    }
    
    /**
     * Try to extract stream data from a player response.
     * This is a suspend function to allow async stream URL decryption.
     */
    private suspend fun tryGetStreamFromResponse(
        client: YouTubeClient,
        response: PlayerResponse,
        videoId: String,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager
    ): ClientStreamResult? {
        if (response.playabilityStatus.status != "OK") {
            Timber.tag(logTag).d("Player response status not OK for ${client.clientName}: ${response.playabilityStatus.status}")
            return null
        }
        
        val format = findFormat(response, audioQuality, connectivityManager)
        if (format == null) {
            Timber.tag(logTag).d("No suitable format found for ${client.clientName}")
            return null
        }
        
        val streamUrl = findUrlOrNullAsync(format, videoId)
        if (streamUrl == null) {
            Timber.tag(logTag).d("Stream URL not found for ${client.clientName}")
            return null
        }
        
        val expiresInSeconds = response.streamingData?.expiresInSeconds
        if (expiresInSeconds == null) {
            Timber.tag(logTag).d("Stream expiration time not found for ${client.clientName}")
            return null
        }
        
        return ClientStreamResult(client, response, format, streamUrl, expiresInSeconds)
    }
    
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX)
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        return format
    }
    
    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }
    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }
    
    /**
     * Async version of [findUrlOrNull] for better performance in coroutine contexts.
     * Wrapper around [NewPipeUtils.getStreamUrlAsync] which reports exceptions.
     */
    private suspend fun findUrlOrNullAsync(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL async for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrlAsync(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully (async)") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL (async)")
                reportException(it)
            }
            .getOrNull()
    }
}
