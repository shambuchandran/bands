package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bands.DestinationScreen
import com.example.bands.data.IceCandidateModel
import com.example.bands.data.MessageModel
import com.example.bands.data.NewMessageInterface
import com.example.bands.webrtc.PeerConnectionObserver
import com.example.bands.webrtc.RTCClient
import com.example.bands.webrtc.SocketRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class CallViewModel @Inject constructor(
    private val application: Application,
    private val socketRepository: SocketRepository
) : ViewModel(), NewMessageInterface {
    //private val visitedRoutes = mutableListOf<String>()

    private var isInitialized = false
    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null

    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
    }
    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
    }
    //fun trackRoute(route: String) { if (!visitedRoutes.contains(route)) { visitedRoutes.add(route) } }
    //fun wasRouteVisited(route: String): Boolean { return visitedRoutes.contains(route) }

    var rtcClient: RTCClient? = null
    private var userName: String? = ""
    private var target: String = ""
    private val gson = Gson()
    val incomingCallerSession: MutableStateFlow<MessageModel?> = MutableStateFlow(null)
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()
    private val _isAudioCall = MutableStateFlow("")
    val isAudioCall: StateFlow<String> = _isAudioCall.asStateFlow()

    private val _isCallAcceptedPending = MutableStateFlow(false)
    val isCallAcceptedPending: StateFlow<Boolean> get() = _isCallAcceptedPending

    fun init(username: String) {
//        if (isInitialized && this.userName == username) return
//        isInitialized = true
//        userName = username
        if (this.userName == username) { isInitialized = true } else { userName = username }
        socketRepository.initSocket(username, this)
        setupRTCClient(username)
    }
    private fun setupRTCClient(username: String) {
        rtcClient = RTCClient(application, username, socketRepository, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    rtcClient?.addIceCandidate(it)
                    socketRepository.sendMessageToSocket(
                        MessageModel("ice_candidate", username, target, mapOf(
                            "sdpMid" to it.sdpMid,
                            "sdpMLineIndex" to it.sdpMLineIndex,
                            "sdpCandidate" to it.sdp
                        ),_isAudioCall.value)
                    )
                }
            }

            override fun onAddStream(p0: MediaStream?) {
                p0?.videoTracks?.firstOrNull()?.let { videoTrack ->
                    remoteSurfaceViewRenderer?.let { videoTrack.addSink(it) }
                }
            }
        })
    }

    fun setCallAcceptedPending(isPending: Boolean) {
        _isCallAcceptedPending.value = isPending
    }

    fun acceptCallIfPending() {
        if (_isCallAcceptedPending.value) {
            acceptCall()
            _isCallAcceptedPending.value = false
        }
    }

    fun startCall(target: String,isAudioCall:String= "false") {
        this.target = target
        Log.d("RTCC","startCall $target $isAudioCall")
        if (rtcClient == null) {
           init(userName!!)
        }
        _isInCall.value=true
        _isAudioCall.value=isAudioCall
        socketRepository.sendMessageToSocket(
            MessageModel(
                "start_call", userName, target, null,isAudioCall
            )
        )


    }
    fun acceptCall() {
        _isInCall.value=true
        if (rtcClient == null) {
            init(userName!!)
        }
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            incomingCallerSession.value?.data.toString()
        )
        Log.d("RTCC","acceptCall $session , ${incomingCallerSession.value?.name} and $incomingCallerSession.value?.isAudioOnly")
        target = incomingCallerSession.value?.name!!
        val isAudioOnly = incomingCallerSession.value?.isAudioOnly ?: "false"
        _isAudioCall.value=isAudioOnly// from session msg
        rtcClient?.onRemoteSessionReceived(session)
        rtcClient?.answer(incomingCallerSession.value?.name!!,isAudioOnly)
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun rejectCall() {
        _isInCall.value=false
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
        socketRepository.sendMessageToSocket(
            MessageModel(
                "end_call", userName, target, null,null
            )
        )
        Log.d("RTCC","rejectCall $incomingCallerSession")
    }

    fun audioButtonClicked(boolean: Boolean) {
        rtcClient?.toggleAudio(boolean)
    }

    fun videoButtonClicked(boolean: Boolean) {
        rtcClient?.toggleCamera(boolean)
    }

    fun cameraSwitchClicked() {
        rtcClient?.switchCamera()
    }

    override fun onCleared() {
        onEndClicked()
        clearSurfaces()
        super.onCleared()
    }



    fun onEndClicked() {
        rtcClient?.endCall()
        stopVideoTrack()
        clearSurfaces()
        rtcClient = null
        _isInCall.value = false
        isInitialized = false
        _isCallAcceptedPending.value = false
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
        socketRepository.sendMessageToSocket(
            MessageModel(
                "end_call", userName, target, null,null
            )
        )
        socketRepository.closeSocket()
        Log.d("RTCC","onEndClicked ${incomingCallerSession.value}")
    }
    fun stopVideoTrack() {
        clearSurfaces()
        rtcClient?.localVideoTrack?.let { videoTrack ->
            videoTrack.setEnabled(false)
            videoTrack.dispose()
            rtcClient?.localVideoTrack = null
        }
    }

    private fun clearSurfaces() {
        localSurfaceViewRenderer?.release()
        localSurfaceViewRenderer?.clearImage()
        localSurfaceViewRenderer = null
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()
        remoteSurfaceViewRenderer = null
    }

    override fun onNewMessage(message: MessageModel) {
        Log.d("RTCC","onNewMessage $message")
        CoroutineScope(Dispatchers.Main).launch {
            //viewModelScope.launch {
            when (message.type) {
                "call_response" -> {
                    if (message.data == "user is not online") {
                        Toast.makeText(application, "user is not available", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val isAudioOnly = message.isAudioOnly ?: _isAudioCall.value
                        rtcClient?.call(target,isAudioOnly)
                    }
                    Log.d("RTCC","call_response $message")
                }
                "offer_received" -> {
                    remoteSurfaceViewRenderer?.isVisible = true
                    viewModelScope.launch {
                        incomingCallerSession.emit(message)
                    }
                    Log.d("RTCC","offer_received $message")
                }
                "answer_received" -> {
                    val session = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        message.data.toString()
                    )
                    rtcClient?.onRemoteSessionReceived(session)
                    Log.d("RTCC","answer_received $message $session")
                }
                "ice_candidate" -> {
                    try {
                        val receivingCandidate =
                            gson.fromJson(gson.toJson(message.data), IceCandidateModel::class.java)
                        rtcClient?.addIceCandidate(
                            IceCandidate(
                                receivingCandidate.sdpMid,
                                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                                receivingCandidate.sdpCandidate
                            )
                        )
                        Log.d("RTCC","ice_candidate $message $receivingCandidate")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("RTCC","ice_candidate error $message")
                    }
                }
                "call_ended" ->{
                    onEndClicked()
                    Log.d("RTCC", "call_ended $message")
                }
            }
        }
    }
}
