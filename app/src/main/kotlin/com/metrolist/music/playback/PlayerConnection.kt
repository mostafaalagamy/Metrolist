package com.metrolist.music.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.currentMetadata
import com.metrolist.music.extensions.getCurrentQueueIndex
import com.metrolist.music.extensions.getQueueWindows
import com.metrolist.music.extensions.metadata
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.utils.JamSessionManager
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    val service = binder.service
    val player = service.player

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val isPlaying =
        combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
            playWhenReady && playbackState != STATE_ENDED
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            player.playWhenReady && player.playbackState != STATE_ENDED
        )
    val mediaMetadata = MutableStateFlow(player.currentMetadata)
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)
    val waitingForNetworkConnection = service.waitingForNetworkConnection

    // Jam Session Manager for listening with friends
    val jamSessionManager = JamSessionManager(context)
    
    private var syncJob: kotlinx.coroutines.Job? = null

    init {
        player.addListener(this)
        
        // Start syncing playback state with jam session (clients only)
        syncJob = scope.launch {
            jamSessionManager.currentSession.collect { session ->
                if (session != null && !jamSessionManager.isHost.value) {
                    // Follow host's playback state (clients only)
                    syncClientPlaybackState()
                }
            }
        }

        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        mediaMetadata.value = player.currentMetadata
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
    }

    fun playQueue(queue: Queue) {
        service.playQueue(queue)
    }

    fun startRadioSeamlessly() {
        service.startRadioSeamlessly()
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
    }

    fun toggleLike() {
        service.toggleLike()
    }

    fun seekToNext() {
        player.seekToNext()
        player.prepare()
        player.playWhenReady = true
        
        // Sync to jam session on manual song change
        if (jamSessionManager.isInSession() && jamSessionManager.isHost.value) {
            jamSessionManager.updatePlaybackState(
                mediaMetadata.value?.id,
                player.currentPosition,
                player.playWhenReady
            )
        }
    }

    fun seekToPrevious() {
        player.seekToPrevious()
        player.prepare()
        player.playWhenReady = true
        
        // Sync to jam session on manual song change
        if (jamSessionManager.isInSession() && jamSessionManager.isHost.value) {
            jamSessionManager.updatePlaybackState(
                mediaMetadata.value?.id,
                player.currentPosition,
                player.playWhenReady
            )
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        playWhenReady.value = newPlayWhenReady
        
        // Sync play/pause state to jam session on manual change
        if (jamSessionManager.isInSession() && jamSessionManager.isHost.value) {
            jamSessionManager.updatePlaybackState(
                mediaMetadata.value?.id,
                player.currentPosition,
                newPlayWhenReady
            )
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        mediaMetadata.value = mediaItem?.metadata
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
        
        // Sync song change to jam session
        if (jamSessionManager.isInSession() && jamSessionManager.isHost.value) {
            jamSessionManager.updatePlaybackState(
                mediaItem?.metadata?.id,
                player.currentPosition,
                player.playWhenReady
            )
        }
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
        
        // Sync queue to jam session on timeline change
        if (jamSessionManager.isInSession() && jamSessionManager.isHost.value) {
            val queueIds = mutableListOf<String>()
            for (i in 0 until timeline.windowCount) {
                val window = Timeline.Window()
                timeline.getWindow(i, window)
                window.mediaItem.metadata?.id?.let { queueIds.add(it) }
            }
            jamSessionManager.updateQueue(queueIds)
        }
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                    !window.isLive ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                    window.isDynamic ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
        syncJob?.cancel()
        jamSessionManager.leaveSession()
    }
    
    /**
     * Sync playback state from jam session (for clients)
     * This only runs when updates are received from the host
     */
    private suspend fun syncClientPlaybackState() {
        var lastSyncedSongId: String? = null
        var lastSyncedQueueHash: Int = 0
        
        jamSessionManager.currentSession.collect { session ->
            if (session != null) {
                // Sync song change
                val currentSongId = mediaMetadata.value?.id
                if (session.currentSongId != null && session.currentSongId != currentSongId) {
                    if (session.currentSongId != lastSyncedSongId) {
                        lastSyncedSongId = session.currentSongId
                        // Find and play the song in the queue
                        for (i in 0 until player.mediaItemCount) {
                            if (player.getMediaItemAt(i).metadata?.id == session.currentSongId) {
                                player.seekTo(i, session.currentPosition)
                                player.playWhenReady = session.isPlaying
                                break
                            }
                        }
                    }
                }
                
                // Sync position if difference is more than 2 seconds
                val positionDiff = kotlin.math.abs(player.currentPosition - session.currentPosition)
                if (positionDiff > 2000 && session.currentSongId == currentSongId) {
                    player.seekTo(session.currentPosition)
                }
                
                // Sync play/pause state
                if (session.isPlaying != player.playWhenReady) {
                    player.playWhenReady = session.isPlaying
                }
                
                // Sync queue if changed
                val queueHash = session.queueSongIds.hashCode()
                if (queueHash != lastSyncedQueueHash && session.queueSongIds.isNotEmpty()) {
                    lastSyncedQueueHash = queueHash
                    // Queue sync would require access to the song database
                    // For now, just log it
                    // TODO: Implement queue synchronization with database access
                }
            }
        }
    }
}
