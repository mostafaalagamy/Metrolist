/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.DecryptionLibrary
import com.metrolist.music.constants.PlayerClient
import com.metrolist.innertube.NewPipeExtractorUtils
import com.metrolist.innertube.PipePipeUtils
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
import okhttp3.OkHttpClient
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    /**
     * Get the YouTubeClient for the given PlayerClient setting.
     * ANDROID_VR is fast (direct URLs), WEB_REMIX is slow (requires JS signature decryption).
     */
    private fun getMainClient(playerClient: PlayerClient): YouTubeClient {
        return when (playerClient) {
            PlayerClient.ANDROID_VR -> ANDROID_VR_1_43_32
            PlayerClient.WEB_REMIX -> WEB_REMIX
        }
    }

    /**
     * Get fallback clients based on the selected main client.
     * If WEB_REMIX is main, put ANDROID_VR first in fallbacks.
     * If ANDROID_VR is main, put WEB_REMIX after other VR clients.
     * 
     * Note: TVHTML5 and TVHTML5_SIMPLY_EMBEDDED_PLAYER are placed earlier in the fallback
     * order as they tend to provide more stable streams for problematic videos.
     */
    private fun getFallbackClients(playerClient: PlayerClient): Array<YouTubeClient> {
        return when (playerClient) {
            PlayerClient.ANDROID_VR -> arrayOf(
                ANDROID_VR_1_61_48,
                WEB_REMIX,
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
            PlayerClient.WEB_REMIX -> arrayOf(
                ANDROID_VR_1_43_32,
                ANDROID_VR_1_61_48,
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
        }
    }

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from the selected main client.
     * Format & stream can be from main client or fallback clients.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        playerClient: PlayerClient = PlayerClient.ANDROID_VR,
        decryptionLibrary: DecryptionLibrary = DecryptionLibrary.NEWPIPE_EXTRACTOR,
    ): Result<PlaybackData> = runCatching {
        val mainClient = getMainClient(playerClient)
        val fallbackClients = getFallbackClients(playerClient)
        
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId, client: ${mainClient.clientName}, decryptionLibrary: $decryptionLibrary")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId, decryptionLibrary)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag).d("Attempting to get player response using main client: ${mainClient.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, mainClient, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        
        // Always use WEB_REMIX for playbackTracking to ensure history sync works
        // ANDROID_VR clients don't support login and may not return valid playbackTracking
        val playbackTracking = if (mainClient != WEB_REMIX) {
            Timber.tag(logTag).d("Fetching playbackTracking from WEB_REMIX for history sync")
            YouTube.player(videoId, playlistId, WEB_REMIX, signatureTimestamp)
                .getOrNull()?.playbackTracking ?: mainPlayerResponse.playbackTracking
        } else {
            mainPlayerResponse.playbackTracking
        }
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until fallbackClients.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first
                client = mainClient
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from main client: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = fallbackClients[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${fallbackClients.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) mainClient.clientName else fallbackClients[clientIndex].clientName}")

                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).d("No suitable format found for client: ${if (clientIndex == -1) mainClient.clientName else fallbackClients[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, decryptionLibrary)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                // Skip validation for main client - it almost always works and
                // skipping the HEAD request saves ~300-500ms on initial playback
                if (clientIndex == -1) {
                    Timber.tag(logTag).d("Using main client directly without validation for faster playback")
                    break
                }

                if (clientIndex == fallbackClients.size - 1) {
                    /** skip [validateStatus] for last client */
                    Timber.tag(logTag).d("Using last fallback client without validation: ${fallbackClients[clientIndex].clientName}")
                    break
                }

                if (validateStatus(streamUrl)) {
                    // working stream found
                    Timber.tag(logTag).d("Stream validated successfully with client: ${if (clientIndex == -1) mainClient.clientName else fallbackClients[clientIndex].clientName}")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${if (clientIndex == -1) mainClient.clientName else fallbackClients[clientIndex].clientName}")
                }
            } else {
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using WEB_REMIX client")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    /**
     * Wrapper around the decryption library's getSignatureTimestamp function which reports exceptions.
     * Uses the selected decryption library with automatic fallback support.
     */
    private fun getSignatureTimestampOrNull(
        videoId: String,
        decryptionLibrary: DecryptionLibrary
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId using $decryptionLibrary")
        
        // Try primary library
        val primaryResult = when (decryptionLibrary) {
            DecryptionLibrary.NEWPIPE_EXTRACTOR -> NewPipeExtractorUtils.getSignatureTimestamp(videoId)
            DecryptionLibrary.PIPEPIPE_EXTRACTOR_API -> PipePipeUtils.getSignatureTimestamp(videoId)
        }
        
        primaryResult.onSuccess { 
            Timber.tag(logTag).d("Signature timestamp obtained: $it")
            return it
        }
        
        // Primary failed, try fallback
        primaryResult.onFailure { primaryError ->
            Timber.tag(logTag).w(primaryError, "Primary library ($decryptionLibrary) failed for signature timestamp")
        }
        
        // Try fallback library
        val fallbackLibrary = getFallbackLibrary(decryptionLibrary)
        Timber.tag(logTag).d("Trying fallback library for signature timestamp: $fallbackLibrary")
        
        val fallbackResult = when (fallbackLibrary) {
            DecryptionLibrary.NEWPIPE_EXTRACTOR -> NewPipeExtractorUtils.getSignatureTimestamp(videoId)
            DecryptionLibrary.PIPEPIPE_EXTRACTOR_API -> PipePipeUtils.getSignatureTimestamp(videoId)
        }
        
        return fallbackResult
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained with fallback: $fallbackLibrary") }
            .onFailure { Timber.tag(logTag).e(it, "All libraries failed to get signature timestamp") }
            .getOrNull()
    }
    
    /**
     * Gets the fallback library based on the primary library.
     */
    private fun getFallbackLibrary(primary: DecryptionLibrary): DecryptionLibrary {
        return when (primary) {
            DecryptionLibrary.NEWPIPE_EXTRACTOR -> DecryptionLibrary.PIPEPIPE_EXTRACTOR_API
            DecryptionLibrary.PIPEPIPE_EXTRACTOR_API -> DecryptionLibrary.NEWPIPE_EXTRACTOR
        }
    }
    
    /**
     * Wrapper around the decryption library's getStreamUrl function which reports exceptions.
     * Uses the selected decryption library with automatic fallback to the alternative library.
     * 
     * Fallback strategy:
     * 1. Try the selected library first
     * 2. If it fails, clear caches and try the alternative library
     * 3. If both fail, return null
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        decryptionLibrary: DecryptionLibrary
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId using $decryptionLibrary")
        
        // Try primary library
        val primaryResult = getStreamUrlFromLibrary(format, videoId, decryptionLibrary)
        
        primaryResult.onSuccess { 
            Timber.tag(logTag).d("Stream URL obtained successfully with primary library: $decryptionLibrary")
            return it
        }
        
        // Primary failed, log and try fallback
        primaryResult.onFailure { primaryError ->
            Timber.tag(logTag).w(primaryError, "Primary library ($decryptionLibrary) failed, attempting fallback")
            clearDecryptionCaches()
        }
        
        // Try fallback library
        val fallbackLibrary = getFallbackLibrary(decryptionLibrary)
        Timber.tag(logTag).d("Trying fallback library: $fallbackLibrary")
        
        val fallbackResult = getStreamUrlFromLibrary(format, videoId, fallbackLibrary)
        
        return fallbackResult
            .onSuccess { 
                Timber.tag(logTag).d("Stream URL obtained successfully with fallback library: $fallbackLibrary")
            }
            .onFailure { fallbackError ->
                Timber.tag(logTag).e(fallbackError, "Both decryption libraries failed for videoId: $videoId")
                reportException(fallbackError)
            }
            .getOrNull()
    }
    
    /**
     * Gets stream URL from a specific library.
     */
    private fun getStreamUrlFromLibrary(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        library: DecryptionLibrary
    ): Result<String> {
        return when (library) {
            DecryptionLibrary.NEWPIPE_EXTRACTOR -> NewPipeExtractorUtils.getStreamUrl(format, videoId)
            DecryptionLibrary.PIPEPIPE_EXTRACTOR_API -> PipePipeUtils.getStreamUrl(format, videoId)
        }
    }
    
    /**
     * Clears all decryption-related caches to force fresh data retrieval.
     * Should be called when decryption errors occur.
     */
    private fun clearDecryptionCaches() {
        try {
            Timber.tag(logTag).d("Clearing decryption caches...")
            NewPipeExtractorUtils.clearCache()
            PipePipeUtils.clearCache()
            Timber.tag(logTag).d("Decryption caches cleared successfully")
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Failed to clear decryption caches")
        }
    }
    
    /**
     * Public method to force clear all caches when playback errors occur.
     * This includes URL caches and decryption caches.
     */
    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing all caches for videoId: $videoId")
        clearDecryptionCaches()
    }
}
