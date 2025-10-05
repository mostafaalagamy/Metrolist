package com.metrolist.music.utils

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils

/**
 * WebSocket Relay-based Jam Session Manager
 * Allows creating/joining sessions and syncing playback state via WebSocket relay server
 */
class JamSessionManager {
    
    data class JamSession(
        val sessionCode: String,
        val hostName: String,
        val participants: List<String> = emptyList(),
        val currentSongId: String? = null,
        val currentPosition: Long = 0,
        val isPlaying: Boolean = false,
        val queueSongIds: List<String> = emptyList()
    )
    
    private val _currentSession = MutableStateFlow<JamSession?>(null)
    val currentSession: StateFlow<JamSession?> = _currentSession.asStateFlow()
    
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null
    private var webSocketSession: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private val client = HttpClient {
        install(WebSockets)
    }
    
    // WebSocket relay server URL - can be configured by users
    private var relayServerUrl = "ws://localhost:8080"
    
    companion object {
        private const val TAG = "JamSessionManager"
    }
    
    /**
     * Configure the WebSocket relay server URL
     * Default is ws://localhost:8080
     */
    fun setRelayServerUrl(url: String) {
        relayServerUrl = url
    }
    
    /**
     * Create a new jam session as host
     */
    fun createSession(hostName: String): String {
        val sessionCode = generateSessionCode()
        
        _currentSession.value = JamSession(
            sessionCode = sessionCode,
            hostName = hostName,
            participants = listOf(hostName)
        )
        _isHost.value = true
        
        // Connect to relay server
        connectToRelay(sessionCode, hostName)
        
        return sessionCode
    }
    
    /**
     * Join an existing jam session
     */
    fun joinSession(sessionCode: String, userName: String): Boolean {
        try {
            _currentSession.value = JamSession(
                sessionCode = sessionCode.uppercase(),
                hostName = "Finding host...",
                participants = listOf(userName)
            )
            _isHost.value = false
            
            // Connect to relay server
            connectToRelay(sessionCode.uppercase(), userName)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join session", e)
            return false
        }
    }
    
    /**
     * Update current playback state in the session and broadcast to peers
     * Only call this on manual changes (song change, seek, play/pause)
     */
    fun updatePlaybackState(songId: String?, position: Long, isPlaying: Boolean) {
        _currentSession.value?.let { session ->
            val updated = session.copy(
                currentSongId = songId,
                currentPosition = position,
                isPlaying = isPlaying
            )
            _currentSession.value = updated
            
            // Broadcast update to peers
            if (_isHost.value) {
                broadcastUpdate(songId, position, isPlaying, session.queueSongIds)
            }
        }
    }
    
    /**
     * Update queue and broadcast to peers
     */
    fun updateQueue(queueSongIds: List<String>) {
        _currentSession.value?.let { session ->
            val updated = session.copy(queueSongIds = queueSongIds)
            _currentSession.value = updated
            
            // Broadcast queue to peers
            if (_isHost.value) {
                broadcastQueue(queueSongIds)
            }
        }
    }
    
    /**
     * Leave the current session
     */
    fun leaveSession() {
        listenerJob?.cancel()
        scope.launch {
            try {
                webSocketSession?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing WebSocket", e)
            }
        }
        webSocketSession = null
        _currentSession.value = null
        _isHost.value = false
    }
    
    /**
     * Check if currently in a session
     */
    fun isInSession(): Boolean = _currentSession.value != null
    
    /**
     * Connect to WebSocket relay server
     */
    private fun connectToRelay(sessionCode: String, userName: String) {
        listenerJob?.cancel()
        
        listenerJob = scope.launch {
            try {
                val url = "$relayServerUrl/$sessionCode"
                Log.d(TAG, "Connecting to WebSocket relay: $url")
                
                webSocketSession = client.webSocketSession(url)
                
                // Announce presence
                val message = if (_isHost.value) {
                    "PRESENCE|$userName"
                } else {
                    "JOIN|$userName"
                }
                webSocketSession?.send(Frame.Text(message))
                
                // Listen for incoming messages
                webSocketSession?.incoming?.receiveAsFlow()?.collect { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val receivedMessage = frame.readText()
                            handleWebSocketMessage(receivedMessage)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to relay server", e)
                // Optionally notify user that connection failed
            }
        }
    }
    
    /**
     * Handle incoming WebSocket messages
     */
    private fun handleWebSocketMessage(message: String) {
        try {
            val parts = message.split("|")
            if (parts.size < 2) return
            
            when (parts[0]) {
                "JOIN" -> {
                    // Someone joined
                    val userName = parts.getOrNull(1) ?: return
                    _currentSession.value?.let { session ->
                        if (!session.participants.contains(userName)) {
                            _currentSession.value = session.copy(
                                participants = session.participants + userName
                            )
                        }
                    }
                }
                "UPDATE" -> {
                    // Playback state update from host
                    if (!_isHost.value) {
                        val songId = parts.getOrNull(1)?.takeIf { it != "null" }
                        val position = parts.getOrNull(2)?.toLongOrNull() ?: 0
                        val isPlaying = parts.getOrNull(3)?.toBoolean() ?: false
                        
                        _currentSession.value?.let { session ->
                            _currentSession.value = session.copy(
                                currentSongId = songId,
                                currentPosition = position,
                                isPlaying = isPlaying
                            )
                        }
                    }
                }
                "QUEUE" -> {
                    // Queue update from host
                    if (!_isHost.value) {
                        val queueData = parts.getOrNull(1) ?: return
                        val queueIds = if (queueData.isNotEmpty()) {
                            queueData.split(",")
                        } else {
                            emptyList()
                        }
                        
                        _currentSession.value?.let { session ->
                            _currentSession.value = session.copy(queueSongIds = queueIds)
                        }
                    }
                }
                "PRESENCE" -> {
                    // Host announcement
                    val hostName = parts.getOrNull(1) ?: return
                    _currentSession.value?.let { session ->
                        if (session.hostName == "Finding host...") {
                            _currentSession.value = session.copy(hostName = hostName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }
    

    
    /**
     * Broadcast playback update to peers via WebSocket
     */
    private fun broadcastUpdate(songId: String?, position: Long, isPlaying: Boolean, queueIds: List<String>) {
        scope.launch {
            try {
                val message = "UPDATE|$songId|$position|$isPlaying"
                sendWebSocketMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting update", e)
            }
        }
    }
    
    /**
     * Broadcast queue update to peers via WebSocket
     */
    private fun broadcastQueue(queueSongIds: List<String>) {
        scope.launch {
            try {
                val queueData = queueSongIds.joinToString(",")
                val message = "QUEUE|$queueData"
                sendWebSocketMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting queue", e)
            }
        }
    }
    
    /**
     * Send message via WebSocket
     */
    private suspend fun sendWebSocketMessage(message: String) {
        try {
            webSocketSession?.send(Frame.Text(message))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WebSocket message", e)
        }
    }
    
    /**
     * Generate a 6-digit session code
     */
    private fun generateSessionCode(): String {
        return RandomStringUtils.insecure().next(6, false, true).uppercase()
    }
}
