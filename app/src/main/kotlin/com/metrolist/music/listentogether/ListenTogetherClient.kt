/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.listentogether

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService
import com.metrolist.music.constants.ListenTogetherServerUrlKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection state for the Listen Together feature
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Room role for the current user
 */
enum class RoomRole {
    HOST,
    GUEST,
    NONE
}

/**
 * Log entry for debugging
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG
}

/**
 * Pending action to execute when connected
 */
sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}

/**
 * Event types for the Listen Together client
 */
sealed class ListenTogetherEvent {
    // Connection events
    data class Connected(val userId: String) : ListenTogetherEvent()
    data object Disconnected : ListenTogetherEvent()
    data class ConnectionError(val error: String) : ListenTogetherEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ListenTogetherEvent()
    
    // Room events
    data class RoomCreated(val roomCode: String, val userId: String) : ListenTogetherEvent()
    data class JoinRequestReceived(val userId: String, val username: String) : ListenTogetherEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: RoomState) : ListenTogetherEvent()
    data class JoinRejected(val reason: String) : ListenTogetherEvent()
    data class UserJoined(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserLeft(val userId: String, val username: String) : ListenTogetherEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : ListenTogetherEvent()
    data class Kicked(val reason: String) : ListenTogetherEvent()
    data class Reconnected(val roomCode: String, val userId: String, val state: RoomState, val isHost: Boolean) : ListenTogetherEvent()
    data class UserReconnected(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserDisconnected(val userId: String, val username: String) : ListenTogetherEvent()
    
    // Playback events
    data class PlaybackSync(val action: PlaybackActionPayload) : ListenTogetherEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : ListenTogetherEvent()
    data class BufferComplete(val trackId: String) : ListenTogetherEvent()
    data class SyncStateReceived(val state: SyncStatePayload) : ListenTogetherEvent()
    
    // Chat events
    data class ChatReceived(
        val userId: String, 
        val username: String, 
        val message: String, 
        val timestamp: Long
    ) : ListenTogetherEvent()
    
    // Error events
    data class ServerError(val code: String, val message: String) : ListenTogetherEvent()
}

/**
 * WebSocket client for Listen Together feature
 */
@Singleton
class ListenTogetherClient @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ListenTogether"
        private const val DEFAULT_SERVER_URL = "https://metroserver.meowery.eu/ws"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 2000L
        private const val PING_INTERVAL_MS = 25000L
        private const val MAX_LOG_ENTRIES = 500
        
        @Volatile
        private var instance: ListenTogetherClient? = null
        
        fun getInstance(): ListenTogetherClient? = instance
        
        fun setInstance(client: ListenTogetherClient) {
            instance = client
        }
    }
    
    init {
        setInstance(this)
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectAttempts = 0
    
    // Session info for reconnection
    private var sessionToken: String? = null
    private var storedUsername: String? = null
    
    // Pending actions to execute when connected
    private var pendingAction: PendingAction? = null
    
    // Wake lock to keep connection alive when in a room
    private var wakeLock: PowerManager.WakeLock? = null

    // State flows
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.NONE)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<JoinRequestPayload>>(emptyList())
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = _pendingJoinRequests.asStateFlow()

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Event flow
    private val _events = MutableSharedFlow<ListenTogetherEvent>()
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private fun getServerUrl(): String {
        return context.dataStore.get(ListenTogetherServerUrlKey, DEFAULT_SERVER_URL)
    }

    private fun log(level: LogLevel, message: String, details: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val entry = LogEntry(timestamp, level, message, details)
        
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)
        
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, "$message ${details ?: ""}")
            LogLevel.WARNING -> Log.w(TAG, "$message ${details ?: ""}")
            LogLevel.DEBUG -> Log.d(TAG, "$message ${details ?: ""}")
            LogLevel.INFO -> Log.i(TAG, "$message ${details ?: ""}")
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Connect to the Listen Together server
     */
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || 
            _connectionState.value == ConnectionState.CONNECTING) {
            log(LogLevel.WARNING, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        log(LogLevel.INFO, "Connecting to server", getServerUrl())

        val request = Request.Builder()
            .url(getServerUrl())
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                log(LogLevel.INFO, "Connected to server")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                startPingJob()
                
                // Try to reconnect to previous session if we have one
                if (sessionToken != null && _roomState.value != null) {
                    log(LogLevel.INFO, "Attempting to reconnect to previous session")
                    sendMessage(MessageTypes.RECONNECT, ReconnectPayload(sessionToken!!))
                } else {
                    // Execute any pending action
                    executePendingAction()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                log(LogLevel.INFO, "Server closing connection", "Code: $code, Reason: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log(LogLevel.INFO, "Connection closed", "Code: $code, Reason: $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                log(LogLevel.ERROR, "Connection failure", t.message)
                handleConnectionFailure(t)
            }
        })
    }
    
    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        
        when (action) {
            is PendingAction.CreateRoom -> {
                log(LogLevel.INFO, "Executing pending create room", action.username)
                sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.username))
            }
            is PendingAction.JoinRoom -> {
                log(LogLevel.INFO, "Executing pending join room", "${action.roomCode} as ${action.username}")
                sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.roomCode.uppercase(), action.username))
            }
        }
    }

    /**
     * Disconnect from the server
     */
    fun disconnect() {
        log(LogLevel.INFO, "Disconnecting from server")
        releaseWakeLock() // Release wake lock when disconnecting
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        // Clear session info on explicit disconnect
        sessionToken = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL_MS)
                sendMessageNoPayload(MessageTypes.PING)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService<PowerManager>()
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Metrolist:ListenTogether"
            )
        }
        if (wakeLock?.isHeld == false) {
            // Acquire with timeout of 30 minutes - will be re-acquired if still in room
            wakeLock?.acquire(30 * 60 * 1000L)
            log(LogLevel.DEBUG, "Wake lock acquired")
        }
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            log(LogLevel.DEBUG, "Wake lock released")
        }
    }

    private fun handleDisconnect() {
        pingJob?.cancel()
        pingJob = null
        
        // Don't clear room state - we might reconnect
        // Only update connection state
        _connectionState.value = ConnectionState.DISCONNECTED
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        // If we have a session, try to reconnect
        if (sessionToken != null && _roomState.value != null) {
            log(LogLevel.INFO, "Connection lost, will attempt to reconnect")
            handleConnectionFailure(Exception("Connection lost"))
        } else {
            scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
        }
    }

    private fun handleConnectionFailure(t: Throwable) {
        pingJob?.cancel()
        pingJob = null
        
        // Always try to reconnect if we have a session token (means we were in a room)
        val shouldReconnect = sessionToken != null || _roomState.value != null || pendingAction != null
        
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
            reconnectAttempts++
            _connectionState.value = ConnectionState.RECONNECTING
            log(LogLevel.INFO, "Attempting reconnect", "Attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")
            
            scope.launch {
                _events.emit(ListenTogetherEvent.Reconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
                delay(RECONNECT_DELAY_MS * reconnectAttempts)
                connect()
            }
        } else {
            _connectionState.value = ConnectionState.ERROR
            // Clear session on final failure
            sessionToken = null
            storedUsername = null
            _roomState.value = null
            _role.value = RoomRole.NONE
            scope.launch { 
                _events.emit(ListenTogetherEvent.ConnectionError(t.message ?: "Unknown error"))
            }
        }
    }

    private fun handleMessage(text: String) {
        log(LogLevel.DEBUG, "Received message", text.take(200))
        
        try {
            val message = json.decodeFromString<Message>(text)
            
            when (message.type) {
                MessageTypes.ROOM_CREATED -> {
                    val payload = json.decodeFromJsonElement<RoomCreatedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = RoomRole.HOST
                    sessionToken = payload.sessionToken
                    _roomState.value = RoomState(
                        roomCode = payload.roomCode,
                        hostId = payload.userId,
                        users = listOf(UserInfo(payload.userId, storedUsername ?: "", true)),
                        isPlaying = false,
                        position = 0,
                        lastUpdate = System.currentTimeMillis()
                    )
                    acquireWakeLock() // Keep connection alive while in room
                    log(LogLevel.INFO, "Room created", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.RoomCreated(payload.roomCode, payload.userId)) }
                }
                
                MessageTypes.JOIN_REQUEST -> {
                    val payload = json.decodeFromJsonElement<JoinRequestPayload>(message.payload!!)
                    _pendingJoinRequests.value = _pendingJoinRequests.value + payload
                    log(LogLevel.INFO, "Join request received", "User: ${payload.username}")
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRequestReceived(payload.userId, payload.username)) }
                }
                
                MessageTypes.JOIN_APPROVED -> {
                    val payload = json.decodeFromJsonElement<JoinApprovedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = RoomRole.GUEST
                    sessionToken = payload.sessionToken
                    _roomState.value = payload.state
                    acquireWakeLock() // Keep connection alive while in room
                    log(LogLevel.INFO, "Joined room", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.JoinApproved(payload.roomCode, payload.userId, payload.state)) }
                }
                
                MessageTypes.JOIN_REJECTED -> {
                    val payload = json.decodeFromJsonElement<JoinRejectedPayload>(message.payload!!)
                    log(LogLevel.WARNING, "Join rejected", payload.reason)
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRejected(payload.reason)) }
                }
                
                MessageTypes.USER_JOINED -> {
                    val payload = json.decodeFromJsonElement<UserJoinedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users + UserInfo(payload.userId, payload.username, false)
                    )
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != payload.userId }
                    log(LogLevel.INFO, "User joined", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserJoined(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_LEFT -> {
                    val payload = json.decodeFromJsonElement<UserLeftPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.filter { it.userId != payload.userId }
                    )
                    log(LogLevel.INFO, "User left", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserLeft(payload.userId, payload.username)) }
                }
                
                MessageTypes.HOST_CHANGED -> {
                    val payload = json.decodeFromJsonElement<HostChangedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        hostId = payload.newHostId,
                        users = _roomState.value!!.users.map { 
                            it.copy(isHost = it.userId == payload.newHostId)
                        }
                    )
                    if (payload.newHostId == _userId.value) {
                        _role.value = RoomRole.HOST
                    }
                    log(LogLevel.INFO, "Host changed", "New host: ${payload.newHostName}")
                    scope.launch { _events.emit(ListenTogetherEvent.HostChanged(payload.newHostId, payload.newHostName)) }
                }
                
                MessageTypes.KICKED -> {
                    val payload = json.decodeFromJsonElement<KickedPayload>(message.payload!!)
                    log(LogLevel.WARNING, "Kicked from room", payload.reason)
                    releaseWakeLock() // Release wake lock when kicked
                    sessionToken = null
                    _roomState.value = null
                    _role.value = RoomRole.NONE
                    scope.launch { _events.emit(ListenTogetherEvent.Kicked(payload.reason)) }
                }
                
                MessageTypes.SYNC_PLAYBACK -> {
                    val payload = json.decodeFromJsonElement<PlaybackActionPayload>(message.payload!!)
                    log(LogLevel.DEBUG, "Playback sync", "Action: ${payload.action}")
                    
                    // Update room state based on action
                    when (payload.action) {
                        PlaybackActions.PLAY -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = true,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.PAUSE -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = false,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.SEEK -> {
                            _roomState.value = _roomState.value?.copy(
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.CHANGE_TRACK -> {
                            _roomState.value = _roomState.value?.copy(
                                currentTrack = payload.trackInfo,
                                isPlaying = false,
                                position = 0
                            )
                        }
                    }
                    
                    scope.launch { _events.emit(ListenTogetherEvent.PlaybackSync(payload)) }
                }
                
                MessageTypes.BUFFER_WAIT -> {
                    val payload = json.decodeFromJsonElement<BufferWaitPayload>(message.payload!!)
                    _bufferingUsers.value = payload.waitingFor
                    log(LogLevel.DEBUG, "Waiting for buffering", "Users: ${payload.waitingFor.size}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferWait(payload.trackId, payload.waitingFor)) }
                }
                
                MessageTypes.BUFFER_COMPLETE -> {
                    val payload = json.decodeFromJsonElement<BufferCompletePayload>(message.payload!!)
                    _bufferingUsers.value = emptyList()
                    log(LogLevel.INFO, "All users buffered", "Track: ${payload.trackId}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferComplete(payload.trackId)) }
                }
                
                MessageTypes.SYNC_STATE -> {
                    val payload = json.decodeFromJsonElement<SyncStatePayload>(message.payload!!)
                    log(LogLevel.INFO, "Sync state received", "Playing: ${payload.isPlaying}, Position: ${payload.position}")
                    scope.launch { _events.emit(ListenTogetherEvent.SyncStateReceived(payload)) }
                }
                
                MessageTypes.CHAT_MESSAGE -> {
                    val payload = json.decodeFromJsonElement<ChatMessagePayload>(message.payload!!)
                    log(LogLevel.DEBUG, "Chat message", "${payload.username}: ${payload.message}")
                    scope.launch { 
                        _events.emit(ListenTogetherEvent.ChatReceived(
                            payload.userId, 
                            payload.username, 
                            payload.message, 
                            payload.timestamp
                        ))
                    }
                }
                
                MessageTypes.ERROR -> {
                    val payload = json.decodeFromJsonElement<ErrorPayload>(message.payload!!)
                    log(LogLevel.ERROR, "Server error", "${payload.code}: ${payload.message}")
                    scope.launch { _events.emit(ListenTogetherEvent.ServerError(payload.code, payload.message)) }
                }
                
                MessageTypes.PONG -> {
                    log(LogLevel.DEBUG, "Pong received")
                }
                
                MessageTypes.RECONNECTED -> {
                    val payload = json.decodeFromJsonElement<ReconnectedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = if (payload.isHost) RoomRole.HOST else RoomRole.GUEST
                    _roomState.value = payload.state
                    acquireWakeLock() // Re-acquire wake lock after reconnection
                    log(LogLevel.INFO, "Reconnected to room", "Code: ${payload.roomCode}, isHost: ${payload.isHost}")
                    scope.launch { _events.emit(ListenTogetherEvent.Reconnected(payload.roomCode, payload.userId, payload.state, payload.isHost)) }
                }
                
                MessageTypes.USER_RECONNECTED -> {
                    val payload = json.decodeFromJsonElement<UserReconnectedPayload>(message.payload!!)
                    // Mark user as connected in the room state
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = true) else user
                        }
                    )
                    log(LogLevel.INFO, "User reconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserReconnected(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_DISCONNECTED -> {
                    val payload = json.decodeFromJsonElement<UserDisconnectedPayload>(message.payload!!)
                    // Mark user as disconnected in the room state
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = false) else user
                        }
                    )
                    log(LogLevel.INFO, "User temporarily disconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserDisconnected(payload.userId, payload.username)) }
                }
                
                else -> {
                    log(LogLevel.WARNING, "Unknown message type", message.type)
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error parsing message", e.message)
        }
    }

    private inline fun <reified T> sendMessage(type: String, payload: T?) {
        val message = if (payload != null) {
            Message(type, json.encodeToJsonElement(payload))
        } else {
            Message(type, null)
        }
        
        val text = json.encodeToString(message)
        log(LogLevel.DEBUG, "Sending message", "$type: ${text.take(200)}")
        
        val success = webSocket?.send(text) ?: false
        if (!success) {
            log(LogLevel.ERROR, "Failed to send message", type)
        }
    }
    
    private fun sendMessageNoPayload(type: String) {
        val message = Message(type, null)
        val text = json.encodeToString(message)
        log(LogLevel.DEBUG, "Sending message", type)
        
        val success = webSocket?.send(text) ?: false
        if (!success) {
            log(LogLevel.ERROR, "Failed to send message", type)
        }
    }

    // Public API methods

    /**
     * Create a new listening room.
     * If not connected, will queue the action and connect first.
     */
    fun createRoom(username: String) {
        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(username))
        } else {
            log(LogLevel.INFO, "Not connected, queueing create room action")
            pendingAction = PendingAction.CreateRoom(username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || 
                _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
            // If CONNECTING or RECONNECTING, the action will be executed when connected
        }
    }

    /**
     * Join an existing room.
     * If not connected, will queue the action and connect first.
     */
    fun joinRoom(roomCode: String, username: String) {
        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(roomCode.uppercase(), username))
        } else {
            log(LogLevel.INFO, "Not connected, queueing join room action")
            pendingAction = PendingAction.JoinRoom(roomCode, username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || 
                _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
            // If CONNECTING or RECONNECTING, the action will be executed when connected
        }
    }

    /**
     * Leave the current room
     */
    fun leaveRoom() {
        sendMessageNoPayload(MessageTypes.LEAVE_ROOM)
        // Clear session info on intentional leave
        sessionToken = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
    }

    /**
     * Approve a join request (host only)
     */
    fun approveJoin(userId: String) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot approve join", "Not host")
            return
        }
        sendMessage(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId))
    }

    /**
     * Reject a join request (host only)
     */
    fun rejectJoin(userId: String, reason: String? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot reject join", "Not host")
            return
        }
        sendMessage(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId, reason))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
    }

    /**
     * Kick a user from the room (host only)
     */
    fun kickUser(userId: String, reason: String? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot kick user", "Not host")
            return
        }
        sendMessage(MessageTypes.KICK_USER, KickUserPayload(userId, reason))
    }

    /**
     * Send a playback action (host only)
     */
    fun sendPlaybackAction(action: String, trackId: String? = null, position: Long? = null, trackInfo: TrackInfo? = null) {
        if (_role.value != RoomRole.HOST) {
            log(LogLevel.ERROR, "Cannot control playback", "Not host")
            return
        }
        sendMessage(MessageTypes.PLAYBACK_ACTION, PlaybackActionPayload(action, trackId, position, trackInfo))
    }

    /**
     * Signal that buffering is complete for the current track
     */
    fun sendBufferReady(trackId: String) {
        sendMessage(MessageTypes.BUFFER_READY, BufferReadyPayload(trackId))
    }

    /**
     * Send a chat message
     */
    fun sendChat(message: String) {
        if (_roomState.value == null) {
            log(LogLevel.ERROR, "Cannot send chat", "Not in room")
            return
        }
        sendMessage(MessageTypes.CHAT, ChatPayload(message))
    }

    /**
     * Request current playback state from server (for guest re-sync)
     */
    fun requestSync() {
        if (_roomState.value == null) {
            log(LogLevel.ERROR, "Cannot request sync", "Not in room")
            return
        }
        log(LogLevel.INFO, "Requesting sync state from server")
        sendMessageNoPayload(MessageTypes.REQUEST_SYNC)
    }

    /**
     * Check if currently in a room
     */
    val isInRoom: Boolean
        get() = _roomState.value != null

    /**
     * Check if current user is host
     */
    val isHost: Boolean
        get() = _role.value == RoomRole.HOST
}
