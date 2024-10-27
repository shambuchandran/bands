package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bands.data.IceCandidateModel
import com.example.bands.data.MessageModel
import com.example.bands.utils.NewMessageInterface
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

    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var rtcClient: RTCClient? = null
    private var userName: String? = ""
    private var targetName: String = ""
    private val gson = Gson()
    val incomingCallerSession: MutableStateFlow<MessageModel?> = MutableStateFlow(null)
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()
    private val _isAudioCall = MutableStateFlow(false)
    val isAudioCall: StateFlow<Boolean> = _isAudioCall.asStateFlow()


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
                            "ice_candidate", username, targetName, candidate, _isAudioCall.value
                        )
                    )
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    if (!isAudioCall.value) {
                        p0?.videoTracks?.get(0)?.addSink(remoteSurfaceViewRenderer)
                    } else {
                        p0?.audioTracks?.get(0)?.setEnabled(true)
                    }
                }
            })
    }
    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
    }
    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
    }


    fun startVideoCall(target: String) {
        targetName = target
        _isInCall.value=true
        _isAudioCall.value = false
        socketRepository.sendMessageToSocket(
            MessageModel("start_call", userName, target, null, false)
        )
    }

    fun startAudioCall(target: String) {
        targetName = target
        _isInCall.value=true
        _isAudioCall.value = true
        socketRepository.sendMessageToSocket(
            MessageModel("start_call", userName, target, null, true)
        )
    }
    fun acceptCall() {
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            incomingCallerSession.value?.data.toString()
        )
        targetName=incomingCallerSession.value?.name!!
        rtcClient?.onRemoteSessionReceived(session)
        rtcClient?.answer(incomingCallerSession.value?.name!!,isAudioCall.value)
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }
    fun rejectCall() {
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
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
    fun onEndClicked() {
        _isInCall.value=false
        rtcClient?.endCall()
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()

    }

    override fun onNewMessage(message: MessageModel) {
        viewModelScope.launch {
            _isAudioCall.value = message.callMode
            when (message.type) {
                "call_response" -> {
                    if (message.data == "user is not online") {
                        //user not reaching
                        Toast.makeText(application, "user not reachable", Toast.LENGTH_SHORT).show()
                    } else {
                        if (_isAudioCall.value) {
                            rtcClient?.call(targetName, true)
                        } else {
                            rtcClient?.call(targetName, false)
                            localSurfaceViewRenderer?.let { rtcClient?.initSurfaceView(it) }
                            remoteSurfaceViewRenderer?.let { rtcClient?.initSurfaceView(it) }
                            localSurfaceViewRenderer?.let { rtcClient?.startLocalVideo(it) }

                        }
                    }
                }

                "answer_received" -> {
                    val session = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        message.data.toString()
                    )
                    rtcClient?.onRemoteSessionReceived(session)
                }

                "offer_received" -> {
                    incomingCallerSession.emit(message)
                    _isAudioCall.value=message.callMode
                    if (!isAudioCall.value) {
                        localSurfaceViewRenderer?.let { rtcClient?.initSurfaceView(it) }
                        remoteSurfaceViewRenderer?.let { rtcClient?.initSurfaceView(it) }
                        localSurfaceViewRenderer?.let { rtcClient?.startLocalVideo(it) }
                    }
                    val session = SessionDescription(
                        SessionDescription.Type.OFFER,
                        message.data.toString()
                    )
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
