package com.my.kizzy.data.gateway

import android.util.Log
import com.my.kizzy.data.gateway.entities.Heartbeat
import com.my.kizzy.data.gateway.entities.Identify.Companion.toIdentifyPayload
import com.my.kizzy.data.gateway.entities.Payload
import com.my.kizzy.data.gateway.entities.Ready
import com.my.kizzy.data.gateway.entities.Resume
import com.my.kizzy.data.gateway.entities.op.OpCode
import com.my.kizzy.data.gateway.entities.op.OpCode.DISPATCH
import com.my.kizzy.data.gateway.entities.op.OpCode.HEARTBEAT
import com.my.kizzy.data.gateway.entities.op.OpCode.HELLO
import com.my.kizzy.data.gateway.entities.op.OpCode.IDENTIFY
import com.my.kizzy.data.gateway.entities.op.OpCode.INVALID_SESSION
import com.my.kizzy.data.gateway.entities.op.OpCode.PRESENCE_UPDATE
import com.my.kizzy.data.gateway.entities.op.OpCode.RECONNECT
import com.my.kizzy.data.gateway.entities.op.OpCode.RESUME
import com.my.kizzy.data.gateway.entities.presence.Presence
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Modified by Zion Huang
 */
open class DiscordWebSocket(
    private val token: String,
) : CoroutineScope {
    private val gatewayUrl = "wss://gateway.discord.gg/?v=10&encoding=json"
    private var websocket: DefaultClientWebSocketSession? = null
    private var sequence = 0
    private var sessionId: String? = null
    private var heartbeatInterval = 0L
    private var resumeGatewayUrl: String? = null
    private var heartbeatJob: Job? = null
    private var connected = false
    private var client: HttpClient = HttpClient {
        install(WebSockets)
    }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Default

    suspend fun connect() {
        launch {
            try {
                Log.i("Gateway", "Connect called")
                val url = resumeGatewayUrl ?: gatewayUrl
                websocket = client.webSocketSession(url)

                // start receiving messages
                websocket!!.incoming.receiveAsFlow()
                    .collect {
                        when (it) {
                            is Frame.Text -> {
                                val jsonString = it.readText()
                                onMessage(json.decodeFromString(jsonString))
                            }

                            else -> {}
                        }
                    }
                handleClose()
            } catch (e: Exception) {
                Log.e("Gateway", e.message ?: "")
                close()
            }
        }
    }

    private suspend fun handleClose() {
        heartbeatJob?.cancel()
        connected = false
        val close = websocket?.closeReason?.await()
        Log.w(
            "Gateway", "Closed with code: ${close?.code}, " +
                    "reason: ${close?.message}, " +
                    "can_reconnect: ${close?.code?.toInt() == 4000}"
        )
        if (close?.code?.toInt() == 4000) {
            delay(200.milliseconds)
            connect()
        } else
            close()
    }

    private suspend fun onMessage(payload: Payload) {
        Log.d("Gateway", "Received op:${payload.op}, seq:${payload.s}, event :${payload.t}")

        payload.s?.let {
            sequence = it
        }
        when (payload.op) {
            DISPATCH -> payload.handleDispatch()
            HEARTBEAT -> sendHeartBeat()
            RECONNECT -> reconnectWebSocket()
            INVALID_SESSION -> handleInvalidSession()
            HELLO -> payload.handleHello()
            else -> {}
        }
    }

    open fun Payload.handleDispatch() {
        when (this.t.toString()) {
            "READY" -> {
                val ready = json.decodeFromJsonElement<Ready>(this.d!!)
                sessionId = ready.sessionId
                resumeGatewayUrl = ready.resumeGatewayUrl + "/?v=10&encoding=json"
                Log.i("Gateway", "resume_gateway_url updated to $resumeGatewayUrl")
                Log.i("Gateway", "session_id updated to $sessionId")
                connected = true
                return
            }

            "RESUMED" -> {
                Log.i("Gateway", "Session Resumed")
            }

            else -> {}
        }
    }

    private suspend inline fun handleInvalidSession() {
        Log.i("Gateway", "Handling Invalid Session")
        Log.d("Gateway", "Sending Identify after 150ms")
        delay(150)
        sendIdentify()
    }

    private suspend inline fun Payload.handleHello() {
        if (sequence > 0 && !sessionId.isNullOrBlank()) {
            sendResume()
        } else {
            sendIdentify()
        }
        heartbeatInterval = json.decodeFromJsonElement<Heartbeat>(this.d!!).heartbeatInterval
        Log.i("Gateway", "Setting heartbeatInterval= $heartbeatInterval")
        startHeartbeatJob(heartbeatInterval)
    }

    private suspend fun sendHeartBeat() {
        Log.i("Gateway", "Sending $HEARTBEAT with seq: $sequence")
        send(
            op = HEARTBEAT,
            d = if (sequence == 0) "null" else sequence.toString(),
        )
    }

    private suspend inline fun reconnectWebSocket() {
        websocket?.close(
            CloseReason(
                code = 4000,
                message = "Attempting to reconnect"
            )
        )
    }

    private suspend fun sendIdentify() {
        Log.i("Gateway", "Sending $IDENTIFY")
        send(
            op = IDENTIFY,
            d = token.toIdentifyPayload()
        )
    }

    private suspend fun sendResume() {
        Log.i("Gateway", "Sending $RESUME")
        send(
            op = RESUME,
            d = Resume(
                seq = sequence,
                sessionId = sessionId,
                token = token
            )
        )
    }

    private fun startHeartbeatJob(interval: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = launch {
            while (isActive) {
                sendHeartBeat()
                delay(interval)
            }
        }
    }

    private fun isSocketConnectedToAccount(): Boolean {
        return connected && websocket?.isActive == true
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun isWebSocketConnected(): Boolean {
        return websocket?.incoming != null && websocket?.outgoing?.isClosedForSend == false
    }

    private suspend inline fun <reified T> send(op: OpCode, d: T?) {
        if (websocket?.isActive == true) {
            val payload = json.encodeToString(
                Payload(
                    op = op,
                    d = json.encodeToJsonElement(d),
                )
            )
            websocket?.send(Frame.Text(payload))
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        this.cancel()
        resumeGatewayUrl = null
        sessionId = null
        connected = false
        runBlocking {
            websocket?.close()
            Log.e("Gateway", "Connection to gateway closed")
        }
    }

    suspend fun sendActivity(presence: Presence) {
        // TODO : Figure out a better way to wait for socket to be connected to account
        while (!isSocketConnectedToAccount()) {
            delay(10.milliseconds)
        }
        Log.i("Gateway", "Sending $PRESENCE_UPDATE")
        send(
            op = PRESENCE_UPDATE,
            d = presence
        )
    }

}