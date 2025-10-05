package com.metrolist.music.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

/**
 * Serverless Jam Session Manager using local network P2P
 * Allows creating/joining sessions and syncing playback state via UDP multicast
 */
class JamSessionManager {
    
    data class JamSession(
        val sessionCode: String,
        val hostName: String,
        val participants: List<String> = emptyList(),
        val currentSongId: String? = null,
        val currentPosition: Long = 0,
        val isPlaying: Boolean = false
    )
    
    private val _currentSession = MutableStateFlow<JamSession?>(null)
    val currentSession: StateFlow<JamSession?> = _currentSession.asStateFlow()
    
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var listenerJob: Job? = null
    private var socket: DatagramSocket? = null
    
    // Use UDP multicast for local network P2P (serverless)
    private val MULTICAST_GROUP = "224.0.0.251" // mDNS multicast address
    private var multicastPort = 0
    
    companion object {
        private const val TAG = "JamSessionManager"
        private const val BASE_PORT = 45000
    }
    
    /**
     * Create a new jam session as host
     */
    fun createSession(hostName: String): String {
        val sessionCode = generateSessionCode()
        multicastPort = BASE_PORT + (sessionCode.hashCode() and 0xFFFF) % 1000
        
        _currentSession.value = JamSession(
            sessionCode = sessionCode,
            hostName = hostName,
            participants = listOf(hostName)
        )
        _isHost.value = true
        
        // Start listening for peers
        startP2PListener()
        
        // Announce presence
        announcePresence(hostName)
        
        return sessionCode
    }
    
    /**
     * Join an existing jam session
     */
    fun joinSession(sessionCode: String, userName: String): Boolean {
        try {
            multicastPort = BASE_PORT + (sessionCode.hashCode() and 0xFFFF) % 1000
            
            _currentSession.value = JamSession(
                sessionCode = sessionCode.uppercase(),
                hostName = "Finding host...",
                participants = listOf(userName)
            )
            _isHost.value = false
            
            // Start listening for host
            startP2PListener()
            
            // Announce joining
            announcePresence(userName)
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join session", e)
            return false
        }
    }
    
    /**
     * Update current playback state in the session and broadcast to peers
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
                broadcastUpdate(songId, position, isPlaying)
            }
        }
    }
    
    /**
     * Leave the current session
     */
    fun leaveSession() {
        listenerJob?.cancel()
        socket?.close()
        socket = null
        _currentSession.value = null
        _isHost.value = false
    }
    
    /**
     * Check if currently in a session
     */
    fun isInSession(): Boolean = _currentSession.value != null
    
    /**
     * Start listening for P2P messages on local network
     */
    private fun startP2PListener() {
        listenerJob?.cancel()
        
        listenerJob = scope.launch {
            try {
                socket = DatagramSocket(multicastPort).apply {
                    broadcast = true
                    reuseAddress = true
                }
                
                val buffer = ByteArray(1024)
                
                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        
                        val message = String(packet.data, 0, packet.length)
                        handleP2PMessage(message, packet.address.hostAddress ?: "")
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error receiving packet", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting listener", e)
            }
        }
    }
    
    /**
     * Handle incoming P2P messages
     */
    private fun handleP2PMessage(message: String, fromAddress: String) {
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
     * Announce presence to the network
     */
    private fun announcePresence(userName: String) {
        scope.launch {
            try {
                val message = if (_isHost.value) {
                    "PRESENCE|$userName"
                } else {
                    "JOIN|$userName"
                }
                sendBroadcast(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error announcing presence", e)
            }
        }
    }
    
    /**
     * Broadcast playback update to peers
     */
    private fun broadcastUpdate(songId: String?, position: Long, isPlaying: Boolean) {
        scope.launch {
            try {
                val message = "UPDATE|$songId|$position|$isPlaying"
                sendBroadcast(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting update", e)
            }
        }
    }
    
    /**
     * Send broadcast message to local network
     */
    private fun sendBroadcast(message: String) {
        try {
            val sendSocket = DatagramSocket()
            sendSocket.broadcast = true
            
            val data = message.toByteArray()
            val address = InetAddress.getByName("255.255.255.255") // Local network broadcast
            val packet = DatagramPacket(data, data.size, address, multicastPort)
            
            sendSocket.send(packet)
            sendSocket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending broadcast", e)
        }
    }
    
    /**
     * Generate a 6-digit session code
     */
    private fun generateSessionCode(): String {
        return RandomStringUtils.insecure().next(6, false, true).uppercase()
    }
}
