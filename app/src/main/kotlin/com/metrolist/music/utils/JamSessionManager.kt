package com.metrolist.music.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.JamSessionBrokerUrlKey
import com.metrolist.music.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * MQTT-based Jam Session Manager
 * Allows creating/joining sessions and syncing playback state via MQTT broker
 * Uses MQTT topics as jam rooms for easy multi-user synchronization
 */
class JamSessionManager(private val context: Context) {
    
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
    private var mqttClient: MqttClient? = null
    private var currentTopic: String? = null
    
    companion object {
        private const val TAG = "JamSessionManager"
        private const val DEFAULT_BROKER_URL = "tcp://broker.hivemq.com:1883" // Public MQTT broker
        private const val TOPIC_PREFIX = "metrolist/jam/"
    }
    
    /**
     * Get the MQTT broker URL from preferences
     */
    private suspend fun getBrokerUrl(): String {
        return context.dataStore.data
            .map { preferences ->
                preferences[JamSessionBrokerUrlKey] ?: DEFAULT_BROKER_URL
            }
            .first()
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
        
        // Connect to MQTT broker and subscribe to topic
        connectToMqttBroker(sessionCode, hostName)
        
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
            
            // Connect to MQTT broker and subscribe to topic
            connectToMqttBroker(sessionCode.uppercase(), userName)
            
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
        scope.launch {
            try {
                mqttClient?.disconnect()
                mqttClient?.close()
                mqttClient = null
                currentTopic = null
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from MQTT", e)
            }
        }
        _currentSession.value = null
        _isHost.value = false
    }
    
    /**
     * Check if currently in a session
     */
    fun isInSession(): Boolean = _currentSession.value != null
    
    /**
     * Connect to MQTT broker and subscribe to session topic
     */
    private fun connectToMqttBroker(sessionCode: String, userName: String) {
        scope.launch {
            try {
                val brokerUrl = getBrokerUrl()
                currentTopic = "$TOPIC_PREFIX$sessionCode"
                
                val clientId = "metrolist_${userName}_${System.currentTimeMillis()}"
                mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
                
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 60
                }
                
                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "MQTT connection lost", cause)
                    }
                    
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        message?.let {
                            val payload = String(it.payload)
                            handleMqttMessage(payload)
                        }
                    }
                    
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Message delivered successfully
                    }
                })
                
                mqttClient?.connect(options)
                mqttClient?.subscribe(currentTopic, 1)
                
                // Announce presence
                val presenceMessage = if (_isHost.value) {
                    "PRESENCE|$userName"
                } else {
                    "JOIN|$userName"
                }
                publishMessage(presenceMessage)
                
                Log.d(TAG, "Connected to MQTT broker: $brokerUrl, topic: $currentTopic")
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to MQTT broker", e)
            }
        }
    }
    
    /**
     * Handle incoming MQTT messages
     */
    private fun handleMqttMessage(message: String) {
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
     * Broadcast playback update to peers via MQTT
     */
    private fun broadcastUpdate(songId: String?, position: Long, isPlaying: Boolean, queueIds: List<String>) {
        scope.launch {
            try {
                val message = "UPDATE|$songId|$position|$isPlaying"
                publishMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting update", e)
            }
        }
    }
    
    /**
     * Broadcast queue update to peers via MQTT
     */
    private fun broadcastQueue(queueSongIds: List<String>) {
        scope.launch {
            try {
                val queueData = queueSongIds.joinToString(",")
                val message = "QUEUE|$queueData"
                publishMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting queue", e)
            }
        }
    }
    
    /**
     * Publish message to MQTT topic
     */
    private fun publishMessage(message: String) {
        try {
            currentTopic?.let { topic ->
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    qos = 1
                    isRetained = false
                }
                mqttClient?.publish(topic, mqttMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing MQTT message", e)
        }
    }
    
    /**
     * Generate a 6-digit session code
     */
    private fun generateSessionCode(): String {
        return RandomStringUtils.insecure().next(6, false, true).uppercase()
    }
}
