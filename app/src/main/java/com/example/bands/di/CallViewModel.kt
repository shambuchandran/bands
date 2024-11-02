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
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class CallViewModel @Inject constructor(
    private val application: Application,
    private val socketRepository: SocketRepository
) : ViewModel(), NewMessageInterface {

    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null

    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
    }

    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
    }


    var rtcClient: RTCClient? = null
    private var userName: String? = ""
    private var target: String = ""
    private val gson = Gson()
    val incomingCallerSession: MutableStateFlow<MessageModel?> = MutableStateFlow(null)

    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()
    private val _isAudioCall = MutableStateFlow(false)
    val isAudioCall: StateFlow<Boolean> = _isAudioCall.asStateFlow()


    private val _isRemoteVideoAvailable = MutableStateFlow(false)
    val isRemoteVideoAvailable: StateFlow<Boolean> get() = _isRemoteVideoAvailable


    fun init(username: String) {
        userName = username
        socketRepository.initSocket(username, this)
        rtcClient =
            RTCClient(application, username, socketRepository, object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
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
                    Log.d("RTCC","ice_candidate")
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)

                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    Log.d("RTCC", "onAddStream called with stream: $p0")

                    if (p0 == null) {
                        Log.e("RTCC", "MediaStream is null")
                        return
                    }

                    if (p0.videoTracks.isNotEmpty()) {
                        val videoTrack = p0.videoTracks[0]
                        // Check if remoteSurfaceViewRenderer is initialized
                        if (remoteSurfaceViewRenderer != null) {
                            Log.d("RTCC", "Adding video track to remote surface")
                            videoTrack.addSink(remoteSurfaceViewRenderer)
                        } else {
                            Log.e("RTCC", "remoteSurfaceViewRenderer is null")
                        }
                    } else {
                        Log.e("RTCC", "No video tracks available in stream")
                    }

                    Log.d("RTCC","onAddStream completed for stream: $p0")
                }
            })
    }


    fun startCall(target: String) {
        this.target = target
        Log.d("RTCC","startCall $target")
        socketRepository.sendMessageToSocket(
            MessageModel(
                "start_call", userName, target, null
            )
        )
    }

    fun acceptCall() {
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            incomingCallerSession.value?.data.toString()
        )
        Log.d("RTCC","acceptCall $session , ${incomingCallerSession.value?.name}")
        //target = incomingCallerSession.value?.name!!
        rtcClient?.onRemoteSessionReceived(session)
        rtcClient?.answer(incomingCallerSession.value?.name!!)
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
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
        localSurfaceViewRenderer?.release()
        localSurfaceViewRenderer?.clearImage()
        localSurfaceViewRenderer = null
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()
        remoteSurfaceViewRenderer = null
        super.onCleared()
    }

    fun onEndClicked() {
        rtcClient?.endCall()
        _isInCall.value = false
        localSurfaceViewRenderer?.release()
        localSurfaceViewRenderer?.clearImage()
        localSurfaceViewRenderer = null
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()
        remoteSurfaceViewRenderer?.isVisible = false
        remoteSurfaceViewRenderer = null
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
        Log.d("RTCC","onEndClicked $incomingCallerSession")
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
                        rtcClient?.call(target)
                    }
                    Log.d("RTCC","call_response ${message.type}")
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
                    Log.d("RTCC","answer_received ${message.type} $session")
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
                        Log.d("RTCC","ice_candidate ${message.type} $receivingCandidate")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("RTCC","ice_candidate error ${message.type}")
                    }
                }
            }
        }
    }
}
