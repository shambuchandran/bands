package com.example.bands.webrtc

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.bands.data.CallMessageModel
import com.example.bands.data.NewMessageInterface
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketRepository @Inject constructor() {
    private var webSocket: WebSocketClient? = null
    private var userName: String? = null
    private val tag = "RTCC"
    private val gson = Gson()
    private var messageInterface: NewMessageInterface? = null

    private var retryCount = 0
    private val maxRetries = 5 // Set a limit on retry attempts
    private val retryDelay = 2000L
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun initSocket(username: String, messageInterface: NewMessageInterface) {
        this.userName = username
        this.messageInterface = messageInterface
        connectSocket()
    }
    private fun connectSocket() {
//        13.228.225.19
//        18.142.128.26
//        54.254.162.138

        webSocket = object : WebSocketClient(URI("wss://localserverwebrtc.onrender.com")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(tag, "WebSocket connected")
                _isConnected.value = true
                retryCount = 0
                sendMessageToSocket(
                    CallMessageModel("store_user", userName, null, null)
                )
            }
            override fun onMessage(message: String?) {
                Log.d(tag, "WebSocket message received: $message")
                try {
                    val callMessageModel = gson.fromJson(message, CallMessageModel::class.java)
                    Log.d(tag, "WebSocket message received DESERIL: $callMessageModel")
                    messageInterface?.onNewMessage(gson.fromJson(message, CallMessageModel::class.java))
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing WebSocket message: ${e.message}")
                }
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(tag, "WebSocket closed: $reason")
                _isConnected.value = false
                if (retryCount < maxRetries) {
                    scheduleReconnect()
                } else {
                    Log.e(tag, "Reached maximum retry attempts. Not reconnecting.")
                }
            }
            override fun onError(ex: Exception?) {
                Log.e(tag, "WebSocket error: $ex")
                _isConnected.value = false
                if (retryCount < maxRetries) {
                    scheduleReconnect()
                }
            }
        }
        webSocket?.connect()
    }

    private fun scheduleReconnect() {
        retryCount++
        Log.d(tag, "Attempting reconnect #$retryCount after delay of $retryDelay ms")
        Handler(Looper.getMainLooper()).postDelayed({
            if (!_isConnected.value) {
                connectSocket()
            }
        }, retryDelay)
    }
    fun sendMessageToSocket(message: CallMessageModel) {
        try {
            if (_isConnected.value) {
                webSocket?.send(gson.toJson(message))
                Log.d(tag, "Message sent to WebSocket: ${gson.toJson(message)}")
            } else {
                Log.e(tag, "WebSocket is not connected. Cannot send message.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message: ${e.message}")
        }
    }
    fun closeSocket() {
        webSocket?.close()
        webSocket = null
        _isConnected.value = false
        Log.d(tag, "WebSocket connection closed.")
    }
}