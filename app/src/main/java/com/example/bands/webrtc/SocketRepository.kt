package com.example.bands.webrtc

import android.util.Log
import com.example.bands.data.MessageModel
import com.example.bands.data.NewMessageInterface
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketRepository @Inject constructor() {

    private var webSocket:WebSocketClient?=null
    private var userName:String?=null
    private val tag ="RTCC"
    private val gson=Gson()


    fun initSocket(username:String,messageInterface: NewMessageInterface){
        userName=username
        // this line will change any time so check and update before run "ws://192.168.1.4:3000"
        webSocket = object :WebSocketClient(URI("ws://192.168.1.6:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(MessageModel(
                    "store_user",userName,null,null
                ))
            }

            override fun onMessage(message: String?) {
                Log.d(tag, "WebSocket message received: $message")
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                }catch (e:Exception){
                    e.printStackTrace()
                    Log.e(tag, "Error parsing WebSocket message: ${e.message}")
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(tag,"onClose $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(tag,"onError $ex")
            }

        }
        webSocket?.connect()
    }

    fun sendMessageToSocket(message: MessageModel) {
        try {
            webSocket?.send(Gson().toJson(message))
            Log.d(tag,"sendMessageToSocket ${Gson().toJson(message)}")
        }catch (e:Exception){
            Log.d(tag,"sendMessageToSocket $e")
        }

    }


}