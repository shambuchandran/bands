package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bands.data.IceCandidateModel
import com.example.bands.data.MessageModel
import com.example.bands.socket.SocketRepository
import com.example.bands.utils.NewMessageInterface
import com.example.bands.webrtc.PeerConnectionObserver
import com.example.bands.webrtc.RTCClient
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class CallViewModel @Inject constructor(
    private val application: Application,
    private val socketRepository: SocketRepository
) : ViewModel(), NewMessageInterface {

    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var rtcClient: RTCClient? = null
    private val gson = Gson()
    private var username: String? = ""
    private var target: String = ""
    val incomingCallerSession: MutableStateFlow<MessageModel?> = MutableStateFlow(null)
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> get() = _isInCall
    private var cachedRemoteVideoTrack: VideoTrack? = null

    fun init(username: String) {
        this.username = username
        socketRepository.initSocket(username, this)
        rtcClient = RTCClient(application, username, socketRepository, object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    _isInCall.value = true
                    rtcClient?.addIceCandidate(p0)
                    val candidate = hashMapOf(
                        "sdpMid" to p0?.sdpMid,
                        "sdpMLineIndex" to p0?.sdpMLineIndex,
                        "sdpCandidate" to p0?.sdp
                    )
                    socketRepository.sendMessageToSocket(
                        MessageModel(
                            "ice_candidate", username, target, candidate
                        )
                    )
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    Log.d("CallViewModel", "onConnectionChange: $newState")
                    viewModelScope.launch {
                        if (newState == PeerConnection.PeerConnectionState.CLOSED ||
                            newState == PeerConnection.PeerConnectionState.DISCONNECTED
                        ) {
                            endCallCleanup()
                        }
                    }
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    _isInCall.value = true
                    cachedRemoteVideoTrack = p0?.videoTracks?.get(0)
                    //attachRemoteVideoIfAvailable()
                    Log.d("CallViewModel", "onAddStream called. Video tracks: ${p0?.videoTracks?.size}")
                    viewModelScope.launch {
                        cachedRemoteVideoTrack = p0?.videoTracks?.get(0) //new
                        retryAttachRemoteVideoTrack()
                    }
                }
            })
    }

    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
        rtcClient?.initSurfaceView(localSurfaceViewRenderer!!, true)
    }

    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
        rtcClient?.initSurfaceView(view, false)
        viewModelScope.launch {
            attachRemoteVideoIfAvailable()
        }
    }

    private suspend fun retryAttachRemoteVideoTrack() {
        viewModelScope.launch {
            var attempts = 0
            while (remoteSurfaceViewRenderer == null && attempts < 10) {
                delay(500)
                attachRemoteVideoIfAvailable()
                attempts++
            }
            if (attempts == 10) {
                Log.e("CallViewModel", "Failed to attach remote video after 10 attempts")
            }
        }
    }
    private fun attachRemoteVideoIfAvailable() {
        val remoteView = remoteSurfaceViewRenderer
        if (remoteSurfaceViewRenderer == null) {
            Log.e("CallViewModel", "Remote surface renderer is null. Cannot attach video track.")
            return
        }
        cachedRemoteVideoTrack?.let { videoTrack ->
            Log.d("CallViewModel", "Attaching remote video track to surface renderer.")
            try {
                videoTrack.addSink(remoteView)
                cachedRemoteVideoTrack = null
                Log.d("CallViewModel", "Remote video track successfully attached.")
            } catch (e: Exception) {
                Log.e("CallViewModel", "Failed to attach remote video track", e)
            }
        } ?: Log.d("CallViewModel", "No remote video track cached.")
    }


    fun startLocalVideo() {
        localSurfaceViewRenderer?.let {
            rtcClient?.startLocalVideo(it)
        }
    }

    fun startCall(target: String) {
        _isInCall.value = true
        if (rtcClient == null) {
            init(username!!)
        }
        this.target = target
        rtcClient?.let { rtc ->
            remoteSurfaceViewRenderer?.let { renderer ->
                rtc.initSurfaceView(renderer, false)
            }
            socketRepository.sendMessageToSocket(
                MessageModel("start_call", username, target, null)
            )
        } ?: run {
            Log.e("CallViewModel", "RTC client is not initialized. Unable to start call.")
        }
    }

    fun acceptCall() {
        _isInCall.value = true
        if (rtcClient == null) {
            init(username!!)
        }
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            incomingCallerSession.value?.data.toString()
        )
        Log.d("CallViewModel", "Accepted call with SDP: ${session.description}")
        rtcClient?.onRemoteSessionReceived(session)
        rtcClient?.answer(incomingCallerSession.value?.name!!)
        target = incomingCallerSession.value?.name!!
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            incomingCallerSession.emit(null)
            _isInCall.emit(false)
        }

    }

    fun onEndClicked() {
        rtcClient?.endCall()
        _isInCall.value = false
        resetCallState()
        endCallCleanup()
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

    private fun resetCallState() {
        remoteSurfaceViewRenderer?.let {
            it.release()
            it.clearImage()
            it.isVisible = false
        }
        localSurfaceViewRenderer?.let {
            it.release()
            it.clearImage()
        }
        rtcClient = null
        cachedRemoteVideoTrack = null
        viewModelScope.launch {
            incomingCallerSession.emit(null)
            _isInCall.emit(false)
        }
    }
    private fun endCallCleanup() {
        viewModelScope.launch {
            cachedRemoteVideoTrack?.removeSink(remoteSurfaceViewRenderer)
            cachedRemoteVideoTrack = null
            remoteSurfaceViewRenderer?.release()
            remoteSurfaceViewRenderer = null
            incomingCallerSession.emit(null)
            _isInCall.emit(false)
        }
    }

    override fun onCleared() {
        resetCallState()
        super.onCleared()
    }


    override fun onNewMessage(message: MessageModel) {
        Log.d("CallViewModel", "Received message: ${message.type}")
        CoroutineScope(Dispatchers.Main).launch {
            //viewModelScope.launch {
            when (message.type) {
                "call_response" -> {
                    if (message.data == "user is not online") {
                        Toast.makeText(application, "user is not available", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        rtcClient?.call(target)
                    }
                }

                "offer_received" -> {
                    remoteSurfaceViewRenderer?.isVisible = true
                    Log.d("CallViewModel", "Offer received from: ${message.name}")
                    viewModelScope.launch {
                        incomingCallerSession.emit(message)
                        Log.d("offer_received", message.toString())
                    }
                }

                "answer_received" -> {
                    val session =
                        SessionDescription(SessionDescription.Type.ANSWER, message.data.toString())
                    rtcClient?.onRemoteSessionReceived(session)
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}