/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.R
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.DecryptionLibrary
import com.metrolist.music.constants.DecryptionLibraryKey
import com.metrolist.music.constants.PlayerClient
import com.metrolist.music.constants.PlayerClientKey
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrappedAudioService(
    private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var player: ExoPlayer? = null
    private var playbackJob: Job? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("WrappedAudioService", "Player error", error)
                        playbackJob?.cancel()
                    }
                })
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        player?.volume = if (_isMuted.value) 0f else 1f
    }

    suspend fun prepareTrack(songId: String?) {
        initPlayer()
        val songUri = getSongUri(songId)
        withContext(Dispatchers.Main) {
            val mediaItem = MediaItem.Builder()
                .setUri(songUri)
                .setMediaId(songId ?: "fallback")
                .build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
        }
    }

    fun playTrack(songId: String?) {
        if (player?.currentMediaItem?.mediaId == songId) {
            Log.d("WrappedAudioService", "Track $songId is already loaded or playing.")
            if (player?.isPlaying == false) player?.play()
            return
        }
        playbackJob?.cancel()

        playbackJob = scope.launch {
            try {
                prepareTrack(songId)
                withContext(Dispatchers.Main) {
                    if (songId != null && songId != "2-p9DM2Xvsc") {
                        player?.seekTo(30_000)
                    } else {
                        player?.seekTo(0)
                    }
                    player?.play()
                    player?.volume = if (_isMuted.value) 0f else 1f
                }
            } catch (e: Exception) {
                Log.e("WrappedAudioService", "Error during playback preparation", e)
            }
        }
    }

    private suspend fun getSongUri(songId: String?): Uri {
        val fallbackUri = Uri.parse("android.resource://${context.packageName}/${R.raw.wrapped_theme}")
        if (songId == null) {
            Log.i("WrappedAudio", "No song ID provided, using fallback audio.")
            return fallbackUri
        }

        return try {
            val audioQuality = context.dataStore.get(com.metrolist.music.constants.AudioQualityKey).let {
                AudioQuality.valueOf(it ?: AudioQuality.AUTO.name)
            }
            val playerClient = context.dataStore.get(PlayerClientKey).let {
                PlayerClient.valueOf(it ?: PlayerClient.ANDROID_VR.name)
            }
            val decryptionLibrary = context.dataStore.get(DecryptionLibraryKey).let {
                DecryptionLibrary.valueOf(it ?: DecryptionLibrary.NEWPIPE_EXTRACTOR.name)
            }
            val playbackData = withContext(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    playerClient = playerClient,
                    decryptionLibrary = decryptionLibrary,
                ).getOrNull()
            }
            val streamUrl = playbackData?.streamUrl
            if (streamUrl.isNullOrBlank()) {
                Log.w("WrappedAudio", "Resolved URL for $songId is null or blank. Using fallback.")
                fallbackUri
            } else {
                Uri.parse(streamUrl)
            }
        } catch (e: Exception) {
            Log.e("WrappedAudio", "Failed to resolve URL for $songId. Using fallback.", e)
            fallbackUri
        }
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun release() {
        playbackJob?.cancel()
        player?.release()
        player = null
        Log.d("WrappedAudioService", "Player released.")
    }
}
