package com.metrolist.music.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.commons.lang3.RandomStringUtils

/**
 * Simple Jam Session Manager for listening with friends
 * Allows creating/joining sessions and sharing playback state
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
        return sessionCode
    }
    
    /**
     * Join an existing jam session
     */
    fun joinSession(sessionCode: String, userName: String): Boolean {
        // In a real implementation, this would connect to a server
        // For simplicity, we'll just create a local session
        _currentSession.value = JamSession(
            sessionCode = sessionCode.uppercase(),
            hostName = "Remote Host",
            participants = listOf("Remote Host", userName)
        )
        _isHost.value = false
        return true
    }
    
    /**
     * Update current playback state in the session
     */
    fun updatePlaybackState(songId: String?, position: Long, isPlaying: Boolean) {
        _currentSession.value?.let { session ->
            _currentSession.value = session.copy(
                currentSongId = songId,
                currentPosition = position,
                isPlaying = isPlaying
            )
        }
    }
    
    /**
     * Leave the current session
     */
    fun leaveSession() {
        _currentSession.value = null
        _isHost.value = false
    }
    
    /**
     * Check if currently in a session
     */
    fun isInSession(): Boolean = _currentSession.value != null
    
    /**
     * Generate a 6-digit session code
     */
    private fun generateSessionCode(): String {
        return RandomStringUtils.insecure().next(6, false, true).uppercase()
    }
}
