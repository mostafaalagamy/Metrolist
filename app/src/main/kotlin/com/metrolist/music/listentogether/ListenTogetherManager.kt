/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.listentogether

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.extensions.currentMetadata
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata.Artist
import com.metrolist.music.models.MediaMetadata.Album
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager that bridges the Listen Together WebSocket client with the music player.
 * Handles syncing playback actions between connected users.
 */
@Singleton
class ListenTogetherManager @Inject constructor(
    private val client: ListenTogetherClient
) {
    companion object {
        private const val TAG = "ListenTogetherManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var playerConnection: PlayerConnection? = null
    private var eventCollectorJob: Job? = null
    private var queueObserverJob: Job? = null
    private var playerListenerRegistered = false
    
    // Whether we're currently syncing (to prevent feedback loops)
    @Volatile
    private var isSyncing = false
    
    // Track the last state we synced to avoid duplicate events
    private var lastSyncedIsPlaying: Boolean? = null
    private var lastSyncedTrackId: String? = null
    
    // Track ID being buffered
    private var bufferingTrackId: String? = null
    
    // Track active sync job to cancel it if a better update arrives
    private var activeSyncJob: Job? = null

    // Pending sync to apply after buffering completes for guest
    private var pendingSyncState: SyncStatePayload? = null

    // Track if a buffer-complete arrived before the pending sync was ready
    private var bufferCompleteReceivedForTrack: String? = null

    // Expose client state
    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val bufferingUsers = client.bufferingUsers
    val logs = client.logs
    val events = client.events
    val pendingSuggestions = client.pendingSuggestions

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost
    val hasPersistedSession: Boolean get() = client.hasPersistedSession
    
    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            
            Log.d(TAG, "Play state changed: $playWhenReady (reason: $reason)")
            
            // ALWAYS ensure track is synced before play/pause
            val currentTrackId = playerConnection?.player?.currentMediaItem?.mediaId
            if (currentTrackId != null && currentTrackId != lastSyncedTrackId) {
                Log.d(TAG, "[SYNC] Sending track change before play state: track = $currentTrackId")
                playerConnection?.player?.currentMetadata?.let { metadata ->
                    sendTrackChangeInternal(metadata)
                    lastSyncedTrackId = currentTrackId
                    // Reset play state since server resets IsPlaying on track change
                    lastSyncedIsPlaying = false
                }
                // ALWAYS send play state after track change if host is playing
                // Server sets IsPlaying=false on track change, so we must send it
                if (playWhenReady) {
                    Log.d(TAG, "[SYNC] Host is playing, sending PLAY after track change")
                    lastSyncedIsPlaying = true
                    val position = playerConnection?.player?.currentPosition ?: 0
                    client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                }
                return
            }
            
            // Only send play/pause if track is already synced
            sendPlayState(playWhenReady)
        }
        
        private fun sendPlayState(playWhenReady: Boolean) {
            val position = playerConnection?.player?.currentPosition ?: 0
            
            if (playWhenReady) {
                Log.d(TAG, "Host sending PLAY at position $position")
                client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                lastSyncedIsPlaying = true
            } else if (!playWhenReady && (lastSyncedIsPlaying == true)) {
                Log.d(TAG, "Host sending PAUSE at position $position")
                client.sendPlaybackAction(PlaybackActions.PAUSE, position = position)
                lastSyncedIsPlaying = false
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            if (mediaItem == null) return
            
            val trackId = mediaItem.mediaId
            if (trackId == lastSyncedTrackId) return
            
            lastSyncedTrackId = trackId
            // Reset play state tracking since server resets IsPlaying on track change
            lastSyncedIsPlaying = false
            
            // Get metadata and send track change
            playerConnection?.player?.currentMetadata?.let { metadata ->
                Log.d(TAG, "Host sending track change: ${metadata.title}")
                sendTrackChange(metadata)
                
                // ALWAYS send PLAY after track change if host is currently playing
                // Server sets IsPlaying=false on track change, so we must re-send it
                val isPlaying = playerConnection?.player?.playWhenReady == true
                if (isPlaying) {
                    Log.d(TAG, "Host is playing during track change, sending PLAY")
                    lastSyncedIsPlaying = true
                    val position = playerConnection?.player?.currentPosition ?: 0
                    client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                }
            }
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isSyncing || !isHost || !isInRoom) return
            
            // Only send seek if it was a user-initiated seek
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                Log.d(TAG, "Host sending SEEK to ${newPosition.positionMs}")
                client.sendPlaybackAction(PlaybackActions.SEEK, position = newPosition.positionMs)
            }
        }
    }

    /**
     * Set the player connection for playback sync.
     * Should be called when PlayerConnection is available.
     */
    fun setPlayerConnection(connection: PlayerConnection?) {
        Log.d(TAG, "setPlayerConnection: ${connection != null}, isInRoom: $isInRoom")
        
        // Remove old listener and callback
        if (playerListenerRegistered && playerConnection != null) {
            playerConnection?.player?.removeListener(playerListener)
            playerListenerRegistered = false
        }
        playerConnection?.shouldBlockPlaybackChanges = null
        playerConnection?.onSkipPrevious = null
        playerConnection?.onSkipNext = null
        playerConnection?.onRestartSong = null
        
        playerConnection = connection
        
        // Set up playback blocking for guests
        connection?.shouldBlockPlaybackChanges = {
            // Block if we're in a room as a guest (not host)
            isInRoom && !isHost
        }
        
        // Add listener if in room
        if (connection != null && isInRoom) {
            connection.player.addListener(playerListener)
            playerListenerRegistered = true
            Log.d(TAG, "Added player listener for room sync")
            
            // Hook up skip actions
            connection.onSkipPrevious = {
                if (isHost && !isSyncing) {
                    Log.d(TAG, "Host Skip Previous triggered")
                    client.sendPlaybackAction(PlaybackActions.SKIP_PREV)
                }
            }
            connection.onSkipNext = {
                if (isHost && !isSyncing) {
                    Log.d(TAG, "Host Skip Next triggered")
                    client.sendPlaybackAction(PlaybackActions.SKIP_NEXT)
                }
            }
            
            // Hook up restart action
            connection.onRestartSong = {
                if (isHost && !isSyncing) {
                    Log.d(TAG, "Host Restart Song triggered (sending 1ms as 0ms workaround)")
                    client.sendPlaybackAction(PlaybackActions.SEEK, position = 1L)
                }
            }
        }

        // Start/stop queue observation based on role
        if (connection != null && isInRoom && isHost) {
            startQueueSyncObservation()
            startHeartbeat()
        } else {
            stopQueueSyncObservation()
            stopHeartbeat()
        }
    }

    /**
     * Initialize event collection. Should be called once at app start.
     */
    fun initialize() {
        Log.d(TAG, "Initializing ListenTogetherManager")
        eventCollectorJob?.cancel()
        eventCollectorJob = scope.launch {
            client.events.collect { event ->
                Log.d(TAG, "Received event: $event")
                handleEvent(event)
            }
        }
        
        // Role change listener
        scope.launch {
            role.collect { newRole ->
                val wasHost = isHost
                if (newRole == RoomRole.HOST && !wasHost && playerConnection != null) {
                    Log.d(TAG, "Role changed to HOST, starting sync services")
                    startQueueSyncObservation()
                    startHeartbeat()
                    // Re-register listener if needed
                    if (!playerListenerRegistered) {
                        playerConnection!!.player.addListener(playerListener)
                        playerListenerRegistered = true
                    }
                } else if (newRole != RoomRole.HOST && wasHost) {
                    Log.d(TAG, "Role changed from HOST, stopping sync services")
                    stopQueueSyncObservation()
                    stopHeartbeat()
                }
            }
        }
    }

    private fun handleEvent(event: ListenTogetherEvent) {
        when (event) {
            is ListenTogetherEvent.Connected -> {
                Log.d(TAG, "Connected to server with userId: ${event.userId}")
            }
            
            is ListenTogetherEvent.RoomCreated -> {
                Log.d(TAG, "Room created: ${event.roomCode}")
                // Register player listener for host
                playerConnection?.player?.let { player ->
                    if (!playerListenerRegistered) {
                        player.addListener(playerListener)
                        playerListenerRegistered = true
                        Log.d(TAG, "Added player listener as host")
                    }
                }
                // Initialize sync state
                lastSyncedIsPlaying = playerConnection?.player?.playWhenReady
                lastSyncedTrackId = playerConnection?.player?.currentMediaItem?.mediaId
                
                // If there's already a track loaded, send it to the server
                playerConnection?.player?.currentMetadata?.let { metadata ->
                    Log.d(TAG, "Room created with existing track: ${metadata.title}")
                    // Send track change so server has the current track info
                    sendTrackChangeInternal(metadata)
                    // If host is already playing, immediately send PLAY with current position
                    val isPlaying = playerConnection?.player?.playWhenReady == true
                    if (isPlaying) {
                        lastSyncedIsPlaying = true
                        val position = playerConnection?.player?.currentPosition ?: 0
                        Log.d(TAG, "Host already playing on room create, sending PLAY at $position")
                        client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                    }
                }
                startQueueSyncObservation()
                startHeartbeat()
            }
            
            is ListenTogetherEvent.JoinApproved -> {
                Log.d(TAG, "Join approved for room: ${event.roomCode}")
                // Apply the full initial state including queue
                applyPlaybackState(
                    currentTrack = event.state.currentTrack,
                    isPlaying = event.state.isPlaying,
                    position = event.state.position,
                    queue = event.state.queue
                    // bypassBuffer=false (default) for initial join buffer sync
                )
            }
            
            is ListenTogetherEvent.PlaybackSync -> {
                Log.d(TAG, "PlaybackSync received: ${event.action.action}")
                // Guests handle all sync actions. Host should also apply queue ops.
                val actionType = event.action.action
                val isQueueOp = actionType == PlaybackActions.QUEUE_ADD ||
                        actionType == PlaybackActions.QUEUE_REMOVE ||
                        actionType == PlaybackActions.QUEUE_CLEAR
                if (!isHost || isQueueOp) {
                    handlePlaybackSync(event.action)
                }
            }
            
            is ListenTogetherEvent.UserJoined -> {
                Log.d(TAG, "[SYNC] User joined: ${event.username}")
                // When a new user joins, host should send current track immediately
                if (isHost) {
                    playerConnection?.player?.currentMetadata?.let { metadata ->
                        Log.d(TAG, "[SYNC] Sending current track to newly joined user: ${metadata.title}")
                        sendTrackChangeInternal(metadata)
                        // If host is currently playing, also send PLAY with current position so the guest jumps to the live position
                        if (playerConnection?.player?.playWhenReady == true) {
                            val pos = playerConnection?.player?.currentPosition ?: 0
                            Log.d(TAG, "[SYNC] Host playing, sending PLAY at $pos for new joiner")
                            client.sendPlaybackAction(PlaybackActions.PLAY, position = pos)
                        }
                        // Don't send play state - let buffering complete first
                    }
                }
            }
            
            is ListenTogetherEvent.BufferWait -> {
                Log.d(TAG, "BufferWait: waiting for ${event.waitingFor.size} users")
            }
            
            is ListenTogetherEvent.BufferComplete -> {
                Log.d(TAG, "BufferComplete for track: ${event.trackId}")
                if (!isHost && bufferingTrackId == event.trackId) {
                    bufferCompleteReceivedForTrack = event.trackId
                    applyPendingSyncIfReady()
                }
            }
            
            is ListenTogetherEvent.SyncStateReceived -> {
                Log.d(TAG, "SyncStateReceived: playing=${event.state.isPlaying}, pos=${event.state.position}, track=${event.state.currentTrack?.id}")
                if (!isHost) {
                    handleSyncState(event.state)
                }
            }
            
            is ListenTogetherEvent.Kicked -> {
                Log.d(TAG, "Kicked from room: ${event.reason}")
                cleanup()
            }
            
            is ListenTogetherEvent.Disconnected -> {
                Log.d(TAG, "Disconnected from server")
                // Don't cleanup on disconnect - we might reconnect
                // cleanup() is called when leaving room intentionally or when kicked
            }
            
            is ListenTogetherEvent.Reconnecting -> {
                Log.d(TAG, "Reconnecting: attempt ${event.attempt}/${event.maxAttempts}")
            }
            
            is ListenTogetherEvent.Reconnected -> {
                Log.d(TAG, "Reconnected to room: ${event.roomCode}, isHost: ${event.isHost}")
                // Re-register player listener
                playerConnection?.player?.let { player ->
                    if (!playerListenerRegistered) {
                        player.addListener(playerListener)
                        playerListenerRegistered = true
                        Log.d(TAG, "Re-added player listener after reconnect")
                    }
                }
                
                // Sync state based on role
                if (event.isHost) {
                    // Host: only send sync if necessary
                    lastSyncedIsPlaying = playerConnection?.player?.playWhenReady
                    lastSyncedTrackId = playerConnection?.player?.currentMediaItem?.mediaId
                    
                    val currentMetadata = playerConnection?.player?.currentMetadata
                    if (currentMetadata != null) {
                        // Check if server already has the right track (from event.state)
                        val serverTrackId = event.state.currentTrack?.id
                        if (serverTrackId != currentMetadata.id) {
                            Log.d(TAG, "Reconnected as host, server track ($serverTrackId) differs from local (${currentMetadata.id}), syncing")
                            sendTrackChangeInternal(currentMetadata)
                        } else {
                            Log.d(TAG, "Reconnected as host, server already has current track $serverTrackId")
                        }
                        
                        // Small delay before sending play state to let connection stabilize
                        scope.launch {
                            delay(500)
                            if (playerConnection?.player?.playWhenReady == true) {
                                val pos = playerConnection?.player?.currentPosition ?: 0
                                Log.d(TAG, "Reconnected host is playing, sending PLAY at $pos")
                                client.sendPlaybackAction(PlaybackActions.PLAY, position = pos)
                            }
                        }
                    }
                } else {
                    // Guest: ALWAYS sync to host's state after reconnection
                    Log.d(TAG, "Reconnected as guest, syncing to host's current state")
                    applyPlaybackState(
                        currentTrack = event.state.currentTrack,
                        isPlaying = event.state.isPlaying,
                        position = event.state.position,
                        queue = event.state.queue,
                        bypassBuffer = true  // Reconnect: bypass buffer protocol
                    )
                    
                    // Immediately request fresh sync after a short delay to catch live position
                    scope.launch {
                        delay(1000)
                        if (isInRoom && !isHost) {
                            Log.d(TAG, "Requesting fresh sync after reconnect")
                            requestSync()
                        }
                    }
                }
            }
            
            is ListenTogetherEvent.UserReconnected -> {
                Log.d(TAG, "User reconnected: ${event.username}")
                // No action needed - reconnected user already synced via reconnect state
            }
            
            is ListenTogetherEvent.UserDisconnected -> {
                Log.d(TAG, "User temporarily disconnected: ${event.username}")
                // User might reconnect, no action needed
            }
            
            is ListenTogetherEvent.ConnectionError -> {
                Log.e(TAG, "Connection error: ${event.error}")
                cleanup()
            }
            
            else -> { /* Other events handled by UI */ }
        }
    }
    
    private fun cleanup() {
        if (playerListenerRegistered) {
            playerConnection?.player?.removeListener(playerListener)
            playerListenerRegistered = false
        }
        stopQueueSyncObservation()
        stopHeartbeat()
        // Note: Don't clear shouldBlockPlaybackChanges callback - it checks isInRoom dynamically
        lastSyncedIsPlaying = null
        lastSyncedTrackId = null
        bufferingTrackId = null
        isSyncing = false
        bufferCompleteReceivedForTrack = null
    }

    private fun applyPendingSyncIfReady() {
        val pending = pendingSyncState ?: return
        val pendingTrackId = pending.currentTrack?.id ?: bufferingTrackId ?: return
        val completeForTrack = bufferCompleteReceivedForTrack

        if (completeForTrack != pendingTrackId) return

        val player = playerConnection?.player ?: return

        Log.d(TAG, "Applying pending sync: track=$pendingTrackId, pos=${pending.position}, play=${pending.isPlaying}")
        isSyncing = true

        val targetPos = pending.position
        if (kotlin.math.abs(player.currentPosition - targetPos) > 100) {
            playerConnection?.seekTo(targetPos)
        }

        if (pending.isPlaying) {
            playerConnection?.play()
        } else {
            playerConnection?.pause()
        }

        scope.launch {
            delay(200)
            isSyncing = false
        }

        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
    }

    private fun handlePlaybackSync(action: PlaybackActionPayload) {
        val player = playerConnection?.player
        if (player == null) {
            Log.w(TAG, "Cannot sync playback - no player connection")
            return
        }
        
        Log.d(TAG, "Handling playback sync: ${action.action}, position: ${action.position}")
        
        isSyncing = true
        
        try {
            when (action.action) {
                PlaybackActions.PLAY -> {
                    val pos = action.position ?: 0L
                    Log.d(TAG, "Guest: PLAY at position $pos")
                    // Seek first for precision, then play
                    if (kotlin.math.abs(player.currentPosition - pos) > 100) {
                        playerConnection?.seekTo(pos)
                    }
                    if (bufferingTrackId == null) {
                        // Start playback immediately for tighter sync
                        playerConnection?.play()
                    }
                }
                
                PlaybackActions.PAUSE -> {
                    val pos = action.position ?: 0L
                    Log.d(TAG, "Guest: PAUSE at position $pos")
                    // Pause first, then seek for accuracy
                    playerConnection?.pause()
                    if (kotlin.math.abs(player.currentPosition - pos) > 100) {
                        playerConnection?.seekTo(pos)
                    }
                }
                
                PlaybackActions.SEEK -> {
                    val pos = action.position ?: 0L
                    Log.d(TAG, "Guest: SEEK to $pos")
                    playerConnection?.seekTo(pos)
                }
                
                PlaybackActions.CHANGE_TRACK -> {
                    action.trackInfo?.let { track ->
                        Log.d(TAG, "Guest: CHANGE_TRACK to ${track.title}, queue size=${action.queue?.size}")
                        
                        // If we have a queue, use it! This is the "smart" sync path.
                        if (action.queue != null && action.queue.isNotEmpty()) {
                            val queueTitle = action.queueTitle
                            applyPlaybackState(
                                currentTrack = track,
                                isPlaying = false, // Will be updated by subsequent PLAY or pending sync
                                position = 0,
                                queue = action.queue,
                                queueTitle = queueTitle
                            )
                        } else {
                            // Fallback to old behavior (network fetch) if no queue provided
                            bufferingTrackId = track.id
                            syncToTrack(track, false, 0)
                        }
                    }
                }
                
                PlaybackActions.SKIP_NEXT -> {
                    Log.d(TAG, "Guest: SKIP_NEXT")
                    playerConnection?.seekToNext()
                }
                
                PlaybackActions.SKIP_PREV -> {
                    Log.d(TAG, "Guest: SKIP_PREV")
                    playerConnection?.seekToPrevious()
                }

                PlaybackActions.QUEUE_ADD -> {
                    val track = action.trackInfo
                    if (track == null) {
                        Log.w(TAG, "QUEUE_ADD missing trackInfo")
                    } else {
                        Log.d(TAG, "Guest: QUEUE_ADD ${track.title}, insertNext=${action.insertNext == true}")
                        scope.launch(Dispatchers.IO) {
                            // Fetch MediaItem via YouTube metadata
                            com.metrolist.innertube.YouTube.queue(listOf(track.id)).onSuccess { list ->
                                val mediaItem = list.firstOrNull()?.toMediaMetadata()?.copy(
                                    suggestedBy = track.suggestedBy
                                )?.toMediaItem()
                                if (mediaItem != null) {
                                    launch(Dispatchers.Main) {
                                        // Allow internal sync to bypass guest restrictions
                                        playerConnection?.allowInternalSync = true
                                        if (action.insertNext == true) {
                                            playerConnection?.playNext(mediaItem)
                                        } else {
                                            playerConnection?.addToQueue(mediaItem)
                                        }
                                        playerConnection?.allowInternalSync = false
                                    }
                                } else {
                                    Log.w(TAG, "QUEUE_ADD failed to resolve media item for ${track.id}")
                                }
                            }.onFailure {
                                Log.e(TAG, "QUEUE_ADD metadata fetch failed", it)
                            }
                        }
                    }
                }

                PlaybackActions.QUEUE_REMOVE -> {
                    val removeId = action.trackId
                    if (removeId.isNullOrEmpty()) {
                        Log.w(TAG, "QUEUE_REMOVE missing trackId")
                    } else {
                        val player = playerConnection?.player
                        if (player != null) {
                            // Find first queue item with matching mediaId after current index
                            val startIndex = player.currentMediaItemIndex + 1
                            var removeIndex = -1
                            val total = player.mediaItemCount
                            for (i in startIndex until total) {
                                val id = player.getMediaItemAt(i).mediaId
                                if (id == removeId) { removeIndex = i; break }
                            }
                            if (removeIndex >= 0) {
                                Log.d(TAG, "Guest: QUEUE_REMOVE index=$removeIndex id=$removeId")
                                player.removeMediaItem(removeIndex)
                            } else {
                                Log.w(TAG, "QUEUE_REMOVE id not found in queue: $removeId")
                            }
                        }
                    }
                }

                PlaybackActions.QUEUE_CLEAR -> {
                    val player = playerConnection?.player
                    if (player != null) {
                        val currentIndex = player.currentMediaItemIndex
                        val count = player.mediaItemCount
                        val itemsAfter = count - (currentIndex + 1)
                        if (itemsAfter > 0) {
                            Log.d(TAG, "Guest: QUEUE_CLEAR removing $itemsAfter items after current")
                            player.removeMediaItems(currentIndex + 1, count - (currentIndex + 1))
                        }
                    }
                }

                PlaybackActions.SYNC_QUEUE -> {
                    val queue = action.queue
                    val queueTitle = action.queueTitle
                    if (queue != null) {
                        Log.d(TAG, "Guest: SYNC_QUEUE size=${queue.size}")
                        // Cancel any pending "smart" sync (e.g. YouTube radio fetch) in favor of this authoritative queue
                        activeSyncJob?.cancel()
                        
                        scope.launch(Dispatchers.Main) {
                            val player = playerConnection?.player ?: return@launch
                            
                            // Map TrackInfo to MediaItems
                            val mediaItems = queue.map { track ->
                                track.toMediaMetadata().toMediaItem()
                            }
                            
                            // Try to find current track in new queue to preserve playback state
                            val currentId = player.currentMediaItem?.mediaId
                            var newIndex = -1
                            if (currentId != null) {
                                newIndex = mediaItems.indexOfFirst { it.mediaId == currentId }
                            }
                            
                            val currentPos = player.currentPosition
                            val wasPlaying = player.isPlaying
                            
                            playerConnection?.allowInternalSync = true
                            if (newIndex != -1) {
                                player.setMediaItems(mediaItems, newIndex, currentPos)
                            } else {
                                player.setMediaItems(mediaItems)
                            }
                            playerConnection?.allowInternalSync = false
                            
                            // Restore playing state if needed
                            if (wasPlaying && !player.isPlaying) {
                                playerConnection?.play()
                            }
                            
                            // Sync queue title
                            playerConnection?.service?.queueTitle = queueTitle
                        }
                    }
                }
            }
        } finally {
            // Minimal delay to prevent feedback loops
            scope.launch {
                delay(200)
                isSyncing = false
            }
        }
    }
    
    private fun handleSyncState(state: SyncStatePayload) {
        Log.d(TAG, "handleSyncState: playing=${state.isPlaying}, pos=${state.position}, track=${state.currentTrack?.id}")
        applyPlaybackState(
            currentTrack = state.currentTrack,
            isPlaying = state.isPlaying,
            position = state.position,
            queue = state.queue,
            bypassBuffer = true  // Manual sync: bypass buffer
        )
    }

    private fun applyPlaybackState(
        currentTrack: TrackInfo?,
        isPlaying: Boolean,
        position: Long,
        queue: List<TrackInfo>?,
        queueTitle: String? = null,  // New param
        bypassBuffer: Boolean = false
    ) {
        val player = playerConnection?.player
        if (player == null) {
            Log.w(TAG, "Cannot apply playback state - no player")
            return
        }

        Log.d(TAG, "Applying playback state: track=${currentTrack?.id}, pos=$position, queue=${queue?.size}, bypassBuffer=$bypassBuffer")

        // Cancel any pending sync job
        activeSyncJob?.cancel()

        // If no track, just pause and clear/set queue
        if (currentTrack == null) {
            Log.d(TAG, "No track in state, pausing")
            scope.launch(Dispatchers.Main) {
                isSyncing = true
                playerConnection?.allowInternalSync = true
                if (queue != null && queue.isNotEmpty()) {
                    val mediaItems = queue.map { it.toMediaMetadata().toMediaItem() }
                    player.setMediaItems(mediaItems)
                } else if (queue != null) {
                    player.clearMediaItems()
                }
                playerConnection?.pause()
                playerConnection?.service?.queueTitle = queueTitle
                playerConnection?.allowInternalSync = false
                isSyncing = false
            }
            return
        }

        bufferingTrackId = currentTrack.id
        
        scope.launch(Dispatchers.Main) {
            isSyncing = true
            playerConnection?.allowInternalSync = true
            
            try {
                // Apply queue/media (same)
                if (queue != null && queue.isNotEmpty()) {
                    val mediaItems = queue.map { it.toMediaMetadata().toMediaItem() }
                    
                    // Find index of current track
                    var startIndex = mediaItems.indexOfFirst { it.mediaId == currentTrack.id }
                    if (startIndex == -1) {
                        Log.w(TAG, "Current track ${currentTrack.id} not found in queue, defaulting to 0")
                        val singleItem = currentTrack.toMediaMetadata().toMediaItem()
                        // Prepend or fallback? Let's just play the track alone if not in queue
                        player.setMediaItems(listOf(singleItem), 0, position)
                    } else {
                        player.setMediaItems(mediaItems, startIndex, position)
                    }
                } else {
                    // No queue provided, fallback to loading just the track (or radio) via syncToTrack logic
                    // But we want to avoid double loading.
                    // If queue is null, we might be in a state where we should fetch radio?
                    // But here we assume authoritative state.
                    Log.d(TAG, "No queue in state, loading single track")
                    // Construct single item
                    val item = currentTrack.toMediaMetadata().toMediaItem()
                    player.setMediaItems(listOf(item), 0, position)
                }
                
                playerConnection?.seekTo(position)  // Always seek immediately to target pos
                
                // Sync queue title
                playerConnection?.service?.queueTitle = queueTitle ?: "Listen Together"
                
                if (bypassBuffer) {
                    // Manual sync/reconnect: apply play/pause immediately, no buffer protocol
                    Log.d(TAG, "Bypass buffer: immediately applying play=$isPlaying at pos=$position")
                    
                    // Wait for player to be ready before seek/play
                    var attempts = 0
                    while (player.playbackState != Player.STATE_READY && attempts < 100) {
                        delay(50)
                        attempts++
                    }
                    if (player.playbackState == Player.STATE_READY) {
                        Log.d(TAG, "Player ready after ${attempts * 50}ms, seeking to $position")
                        player.seekTo(position)
                        if (isPlaying) {
                            playerConnection?.play()
                            Log.d(TAG, "Bypass: PLAY issued")
                        } else {
                            playerConnection?.pause()
                            Log.d(TAG, "Bypass: PAUSE issued")
                        }
                    } else {
                        Log.w(TAG, "Player not ready after 5s timeout during bypass sync")
                    }
                    
                    // Clear sync state
                    pendingSyncState = null
                    bufferingTrackId = null
                    bufferCompleteReceivedForTrack = null
                } else {
                    // Normal sync: pause, store pending, send buffer_ready
                    playerConnection?.pause()
                    pendingSyncState = SyncStatePayload(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        position = position,
                        lastUpdate = System.currentTimeMillis()
                    )
                    applyPendingSyncIfReady()
                    client.sendBufferReady(currentTrack.id)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error applying playback state", e)
            } finally {
                playerConnection?.allowInternalSync = false
                delay(200)
                isSyncing = false
            }
        }
    }

    private fun syncToTrack(track: TrackInfo, shouldPlay: Boolean, position: Long) {
        Log.d(TAG, "syncToTrack: ${track.title}, play: $shouldPlay, pos: $position")

        // Track which buffer-complete we expect for this load
        bufferingTrackId = track.id
        
        activeSyncJob?.cancel()
        activeSyncJob = scope.launch(Dispatchers.IO) {
            try {
                // Use YouTube API to play the track by ID
                YouTube.queue(listOf(track.id)).onSuccess { queue ->
                    Log.d(TAG, "Got queue for track ${track.id}")
                    launch(Dispatchers.Main) {
                        isSyncing = true
                        // Allow internal sync to bypass playback blocking for guests
                        playerConnection?.allowInternalSync = true
                        playerConnection?.playQueue(
                            YouTubeQueue(
                                endpoint = WatchEndpoint(videoId = track.id),
                                preloadItem = queue.firstOrNull()?.toMediaMetadata()
                            )
                        )
                        playerConnection?.service?.queueTitle = "Listen Together" // Set default title
                        playerConnection?.allowInternalSync = false
                        
                        // Wait for player to be ready - monitor actual player state
                        var waitCount = 0
                        while (waitCount < 40) { // Max 2 seconds (40 * 50ms)
                            val player = playerConnection?.player
                            if (player != null && player.playbackState == Player.STATE_READY) {
                                Log.d(TAG, "Player ready after ${waitCount * 50}ms")
                                break
                            }
                            delay(50)
                            waitCount++
                        }

                        // Do NOT seek here; defer the exact seek until after the server signals buffer-complete
                        // Ensure paused state before signaling ready
                        playerConnection?.pause()

                        // Store pending sync (guest will apply seek + play/pause after BufferComplete)
                        pendingSyncState = SyncStatePayload(
                            currentTrack = track,
                            isPlaying = shouldPlay,
                            position = position,
                            lastUpdate = System.currentTimeMillis()
                        )

                        // Apply immediately if buffer-complete already arrived
                        applyPendingSyncIfReady()

                        // Signal we're ready to play
                        client.sendBufferReady(track.id)
                        Log.d(TAG, "Sent buffer ready for ${track.id}, pending sync stored: pos=$position, play=$shouldPlay")

                        // Minimal delay before accepting sync commands
                        delay(100)
                        isSyncing = false
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to load track ${track.id}", e)
                    playerConnection?.allowInternalSync = false
                    isSyncing = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to track", e)
                playerConnection?.allowInternalSync = false
                isSyncing = false
            }
        }
    }

    // Public API for host actions

    /**
     * Connect to the Listen Together server
     */
    fun connect() {
        Log.d(TAG, "Connecting to server")
        client.connect()
    }

    /**
     * Disconnect from the server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from server")
        cleanup()
        client.disconnect()
    }

    /**
     * Create a new room
     */
    fun createRoom(username: String) {
        Log.d(TAG, "Creating room with username: $username")
        client.createRoom(username)
    }

    /**
     * Join an existing room
     */
    fun joinRoom(roomCode: String, username: String) {
        Log.d(TAG, "Joining room $roomCode as $username")
        client.joinRoom(roomCode, username)
    }

    /**
     * Leave the current room
     */
    fun leaveRoom() {
        Log.d(TAG, "Leaving room")
        cleanup()
        client.leaveRoom()
    }

    /**
     * Approve a join request
     */
    fun approveJoin(userId: String) = client.approveJoin(userId)

    /**
     * Reject a join request
     */
    fun rejectJoin(userId: String, reason: String? = null) = client.rejectJoin(userId, reason)

    /**
     * Kick a user
     */
    fun kickUser(userId: String, reason: String? = null) = client.kickUser(userId, reason)

    /**
     * Send track change (host only) - called when host changes track
     */
    fun sendTrackChange(metadata: MediaMetadata) {
        if (!isHost || isSyncing) return
        sendTrackChangeInternal(metadata)
    }
    
    /**
     * Internal track change - bypasses isSyncing check for initial state sync
     */
    private fun sendTrackChangeInternal(metadata: MediaMetadata) {
        if (!isHost) return
        
        // Use a default duration of 3 minutes if duration is 0 or negative
        val durationMs = if (metadata.duration > 0) metadata.duration.toLong() * 1000 else 180000L
        
        val trackInfo = TrackInfo(
            id = metadata.id,
            title = metadata.title,
            artist = metadata.artists.joinToString(", ") { it.name },
            album = metadata.album?.title,
            duration = durationMs,
            thumbnail = metadata.thumbnailUrl,
            suggestedBy = metadata.suggestedBy
        )
        
        Log.d(TAG, "Sending track change: ${trackInfo.title}, duration: $durationMs")
        
        // Also grab current queue to send along with track change
        val currentQueue = playerConnection?.queueWindows?.value?.map { it.toTrackInfo() }
        val currentTitle = playerConnection?.queueTitle?.value
        
        client.sendPlaybackAction(
            PlaybackActions.CHANGE_TRACK,
            queueTitle = currentTitle,
            trackInfo = trackInfo,
            queue = currentQueue
        )
    }

    private fun startQueueSyncObservation() {
        if (queueObserverJob?.isActive == true) return
    
        Log.d(TAG, "Starting queue sync observation")
        queueObserverJob = scope.launch {
            playerConnection?.queueWindows
                ?.map { windows ->
                    windows.map { it.toTrackInfo() }
                }
                ?.distinctUntilChanged()
                ?.collectLatest { tracks ->
                    if (!isHost || !isInRoom || isSyncing) return@collectLatest
                
                    delay(500) // Debounce rapid playlist manipulations
                
                    Log.d(TAG, "Sending SYNC_QUEUE with ${tracks.size} items")
                    client.sendPlaybackAction(
                        PlaybackActions.SYNC_QUEUE,
                        queueTitle = playerConnection?.queueTitle?.value,
                        queue = tracks
                    )
                }
        }
    }

    private fun androidx.media3.common.Timeline.Window.toTrackInfo(): TrackInfo {
        val metadata = mediaItem.metadata ?: return TrackInfo("unknown", "Unknown", "Unknown", "", 0, "")
        val durationMs = if (metadata.duration > 0) metadata.duration.toLong() * 1000 else 180000L
        return TrackInfo(
            id = metadata.id,
            title = metadata.title,
            artist = metadata.artists.joinToString(", ") { it.name },
            album = metadata.album?.title,
            duration = durationMs,
            thumbnail = metadata.thumbnailUrl,
            suggestedBy = metadata.suggestedBy
        )
    }

    private fun stopQueueSyncObservation() {
        queueObserverJob?.cancel()
        queueObserverJob = null
    }

    private fun TrackInfo.toMediaMetadata(): MediaMetadata {
        return MediaMetadata(
            id = id,
            title = title,
            artists = listOf(Artist(id = "", name = artist)),
            album = if (album != null) Album(id = "", title = album) else null,
            duration = (duration / 1000).toInt(),
            thumbnailUrl = thumbnail,
            suggestedBy = suggestedBy
        )
    }

    /**
     * Signal buffer ready
     */
    fun sendBufferReady(trackId: String) {
        bufferingTrackId = null
        client.sendBufferReady(trackId)
    }

    /**
     * Send chat message
     */
    fun sendChat(message: String) = client.sendChat(message)

    /**
     * Request sync state from server (for guests to re-sync)
     * Call this when a guest presses play/pause to sync with host
     */
    fun requestSync() {
        if (!isInRoom || isHost) {
            Log.d(TAG, "requestSync: not applicable (isInRoom=$isInRoom, isHost=$isHost)")
            return
        }
        Log.d(TAG, "Requesting sync from server")
        client.requestSync()
    }

    /**
     * Clear logs
     */
    fun clearLogs() = client.clearLogs()

    // Suggestions API

    /**
     * Suggest the given track to the host (guest only)
     */
    fun suggestTrack(track: TrackInfo) = client.suggestTrack(track)

    /**
     * Approve a suggestion (host only)
     */
    fun approveSuggestion(suggestionId: String) {
        if (!isHost) return
        // Send approval; server will insert-next and broadcast once
        client.approveSuggestion(suggestionId)
    }

    /**
     * Reject a suggestion (host only)
     */
    fun rejectSuggestion(suggestionId: String, reason: String? = null) = client.rejectSuggestion(suggestionId, reason)
    
    /**
     * Force reconnection to server (for manual recovery)
     */
    fun forceReconnect() {
        Log.d(TAG, "Forcing reconnection")
        client.forceReconnect()
    }
    
    /**
     * Get persisted room code if available
     */
    fun getPersistedRoomCode(): String? = client.getPersistedRoomCode()
    
    /**
     * Get current session age
     */
    fun getSessionAge(): Long = client.getSessionAge()

    // Heartbeat timer
    private var heartbeatJob: Job? = null

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (heartbeatJob?.isActive == true && isInRoom && isHost) {
                delay(15000L) // 15 seconds
                playerConnection?.player?.let { player ->
                    if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                        val pos = player.currentPosition
                        Log.d(TAG, "Host heartbeat: sending PLAY at pos $pos")
                        client.sendPlaybackAction(PlaybackActions.PLAY, position = pos)
                    }
                }
            }
        }
        Log.d(TAG, "Host heartbeat started (15s interval)")
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(TAG, "Host heartbeat stopped")
    }
}
