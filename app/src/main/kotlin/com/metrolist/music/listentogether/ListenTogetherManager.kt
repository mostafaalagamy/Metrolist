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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var playerListenerRegistered = false
    
    // Whether we're currently syncing (to prevent feedback loops)
    @Volatile
    private var isSyncing = false
    
    // Track the last state we synced to avoid duplicate events
    private var lastSyncedIsPlaying: Boolean? = null
    private var lastSyncedTrackId: String? = null
    
    // Track ID being buffered
    private var bufferingTrackId: String? = null

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
            // Only sync if state actually changed
            if (playWhenReady != lastSyncedIsPlaying) {
                lastSyncedIsPlaying = playWhenReady
                val position = playerConnection?.player?.currentPosition ?: 0
                
                if (playWhenReady) {
                    Log.d(TAG, "Host sending PLAY at position $position")
                    client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                } else {
                    Log.d(TAG, "Host sending PAUSE at position $position")
                    client.sendPlaybackAction(PlaybackActions.PAUSE, position = position)
                }
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
                }
            }
            
            is ListenTogetherEvent.JoinApproved -> {
                Log.d(TAG, "Join approved for room: ${event.roomCode}")
                // Only sync if host is actively playing (not paused or empty)
                if (event.state.isPlaying && event.state.currentTrack != null) {
                    val track = event.state.currentTrack
                    Log.d(TAG, "Host is playing: ${track.title} - syncing immediately")
                    syncToTrack(track, true, event.state.position)
                } else if (event.state.currentTrack != null) {
                    // Track exists but paused - load but don't play
                    val track = event.state.currentTrack
                    Log.d(TAG, "Host has track paused: ${track.title} - loading but not playing")
                    syncToTrack(track, false, event.state.position)
                } else {
                    // No track - do nothing
                    Log.d(TAG, "No current track in room - waiting for host action")
                }
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
                    bufferingTrackId = null
                    // Playback will start via sync_playback PLAY action
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
                    // Host: send current track info to server to sync others
                    lastSyncedIsPlaying = playerConnection?.player?.playWhenReady
                    lastSyncedTrackId = playerConnection?.player?.currentMediaItem?.mediaId
                    playerConnection?.player?.currentMetadata?.let { metadata ->
                        Log.d(TAG, "Reconnected as host, sending current track: ${metadata.title}")
                        sendTrackChangeInternal(metadata)
                    }
                } else {
                    // Guest: sync to host's state
                    if (event.state.currentTrack != null) {
                        val currentTrackId = playerConnection?.player?.currentMediaItem?.mediaId
                        if (currentTrackId != event.state.currentTrack.id) {
                            Log.d(TAG, "Reconnected as guest, syncing to track: ${event.state.currentTrack.title}")
                            syncToTrack(event.state.currentTrack, event.state.isPlaying, event.state.position)
                        } else {
                            // Same track, just sync position/play state
                            isSyncing = true
                            if (kotlin.math.abs((playerConnection?.player?.currentPosition ?: 0) - event.state.position) > 1000) {
                                playerConnection?.seekTo(event.state.position)
                            }
                            if (event.state.isPlaying) {
                                playerConnection?.play()
                            } else {
                                playerConnection?.pause()
                            }
                            scope.launch {
                                delay(200)
                                isSyncing = false
                            }
                        }
                    }
                }
            }
            
            is ListenTogetherEvent.UserReconnected -> {
                Log.d(TAG, "User reconnected: ${event.username}")
                // If host, send current state to the reconnected user
                if (isHost) {
                    playerConnection?.player?.currentMetadata?.let { metadata ->
                        Log.d(TAG, "Sending current track to reconnected user: ${metadata.title}")
                        sendTrackChangeInternal(metadata)
                    }
                }
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
        // Note: Don't clear shouldBlockPlaybackChanges callback - it checks isInRoom dynamically
        lastSyncedIsPlaying = null
        lastSyncedTrackId = null
        bufferingTrackId = null
        isSyncing = false
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
                    Log.d(TAG, "Guest: PLAY at position ${action.position}")
                    // Seek first for precision, then play
                    action.position?.let { pos ->
                        if (kotlin.math.abs(player.currentPosition - pos) > 100) {
                            playerConnection?.seekTo(pos)
                        }
                    }
                    if (bufferingTrackId == null) {
                        // Start playback immediately for tighter sync
                        playerConnection?.play()
                    }
                }
                
                PlaybackActions.PAUSE -> {
                    Log.d(TAG, "Guest: PAUSE at position ${action.position}")
                    // Pause first, then seek for accuracy
                    playerConnection?.pause()
                    action.position?.let { pos ->
                        if (kotlin.math.abs(player.currentPosition - pos) > 100) {
                            playerConnection?.seekTo(pos)
                        }
                    }
                }
                
                PlaybackActions.SEEK -> {
                    Log.d(TAG, "Guest: SEEK to ${action.position}")
                    action.position?.let { 
                        playerConnection?.seekTo(it)
                    }
                }
                
                PlaybackActions.CHANGE_TRACK -> {
                    action.trackInfo?.let { track ->
                        Log.d(TAG, "Guest: CHANGE_TRACK to ${track.title}")
                        bufferingTrackId = track.id
                        syncToTrack(track, false, 0)
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
                                val mediaItem = list.firstOrNull()?.toMediaMetadata()?.toMediaItem()
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
        val player = playerConnection?.player
        if (player == null) {
            Log.w(TAG, "Cannot handle sync state - no player connection")
            return
        }
        
        Log.d(TAG, "Handling sync state: track=${state.currentTrack?.id}, playing=${state.isPlaying}, pos=${state.position}")
        
        val currentTrackId = player.currentMediaItem?.mediaId
        
        // If track is different, load the new track
        if (state.currentTrack != null && state.currentTrack.id != currentTrackId) {
            Log.d(TAG, "Sync state: different track, loading ${state.currentTrack.title}")
            bufferingTrackId = state.currentTrack.id
            syncToTrack(state.currentTrack, state.isPlaying, state.position)
        } else {
            // Same track - just sync position and play state
            isSyncing = true
            
            // Seek to position
            if (kotlin.math.abs(player.currentPosition - state.position) > 500) {
                Log.d(TAG, "Sync state: seeking to ${state.position}")
                playerConnection?.seekTo(state.position)
            }
            
            // Sync play state
            if (state.isPlaying && !player.playWhenReady) {
                Log.d(TAG, "Sync state: starting playback")
                playerConnection?.play()
            } else if (!state.isPlaying && player.playWhenReady) {
                Log.d(TAG, "Sync state: pausing playback")
                playerConnection?.pause()
            }
            
            scope.launch {
                delay(200)
                isSyncing = false
            }
        }
    }

    private fun syncToTrack(track: TrackInfo, shouldPlay: Boolean, position: Long) {
        Log.d(TAG, "syncToTrack: ${track.title}, play: $shouldPlay, pos: $position")
        
        scope.launch(Dispatchers.IO) {
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
                        // Reset flag after playQueue call
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
                        
                        // Seek to exact position
                        if (position > 0) {
                            playerConnection?.seekTo(position)
                        }
                        
                        // Ensure paused state before signaling ready
                        playerConnection?.pause()
                        
                        // Signal we're ready to play
                        client.sendBufferReady(track.id)
                        Log.d(TAG, "Sent buffer ready for ${track.id}")
                        
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
            thumbnail = metadata.thumbnailUrl
        )
        
        Log.d(TAG, "Sending track change: ${trackInfo.title}, duration: $durationMs")
        client.sendPlaybackAction(
            PlaybackActions.CHANGE_TRACK,
            trackInfo = trackInfo
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
}
