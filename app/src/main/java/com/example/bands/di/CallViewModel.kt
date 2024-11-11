package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.data.CALLLOG
import com.example.bands.data.CallLog
import com.example.bands.data.IceCandidateModel
import com.example.bands.data.CallMessageModel
import com.example.bands.data.NewMessageInterface
import com.example.bands.webrtc.PeerConnectionObserver
import com.example.bands.webrtc.RTCClient
import com.example.bands.webrtc.SocketRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
enum class CallStatus {
    MISSED,
    REJECTED,
    NOTINCALL,
    ONGOING,
    COMPLETED
}

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class CallViewModel @Inject constructor(
    private val application: Application,
    private val socketRepository: SocketRepository,
    private val fireStore: FirebaseFirestore,
) : ViewModel(), NewMessageInterface {

    private val _callStatus = MutableStateFlow<CallStatus?>(null)
    val callStatus: StateFlow<CallStatus?> = _callStatus.asStateFlow()
    private var isInitialized = false
    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null
    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
    }
    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
    }
    fun setCallStatus(status: CallStatus) {
        _callStatus.value = status
    }

    var rtcClient: RTCClient? = null
    private var userName: String? = ""
    private var target: String = ""
    private val gson = Gson()
    val incomingCallerSession: MutableStateFlow<CallMessageModel?> = MutableStateFlow(null)
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()
    private val _isAudioCall = MutableStateFlow("")
    val isAudioCall: StateFlow<String> = _isAudioCall.asStateFlow()

    private val _isCallAcceptedPending = MutableStateFlow(false)
    val isCallAcceptedPending: StateFlow<Boolean> get() = _isCallAcceptedPending
    private val _callLogs = MutableStateFlow<List<CallLog>>(emptyList())
    val callLogs: StateFlow<List<CallLog>> = _callLogs.asStateFlow()


    fun init(username: String) {
        if (this.userName == username) { isInitialized = true } else { userName = username }
        socketRepository.initSocket(username, this)
        setupRTCClient(username)
        setCallStatus(CallStatus.NOTINCALL)
    }
    private fun setupRTCClient(username: String) {
        rtcClient = RTCClient(application, username, socketRepository, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    rtcClient?.addIceCandidate(it)
                    socketRepository.sendMessageToSocket(
                        CallMessageModel("ice_candidate", username, target, mapOf(
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
            CallMessageModel(
                "start_call", userName, target, null,isAudioCall
            )
        )


    }
    fun acceptCall() {
        _isInCall.value=true
        if (rtcClient == null) {
            init(userName!!)
        }
        setCallStatus(CallStatus.ONGOING)
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
        val callLog = createCallLog(CallStatus.ONGOING)
        saveCallLog(callLog)
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun rejectCall() {
        _isInCall.value=false
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
        val callLog = createCallLog(CallStatus.REJECTED)
        saveCallLog(callLog)
        socketRepository.sendMessageToSocket(
            CallMessageModel(
                "end_call", userName, target, CallStatus.REJECTED.name,null
            )
        )
        setCallStatus(CallStatus.COMPLETED)
        Log.d("RTCC","rejectCall $incomingCallerSession")
    }
    fun handleMissedCall() {
        val callLog = createCallLog(CallStatus.MISSED)
        saveCallLog(callLog)
        socketRepository.sendMessageToSocket(
            CallMessageModel(
                "end_call", userName, target, CallStatus.MISSED.name, null
            )
        )
        setCallStatus(CallStatus.COMPLETED)
    }
    private fun createCallLog(status: CallStatus): CallLog {
        return CallLog(
            caller = userName!!,
            target = target,
            callType = if (_isAudioCall.value == "true") "audio" else "video",
            startTime = System.currentTimeMillis(),
            status = status.name
        )
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
        clearSurfaces()
        super.onCleared()
    }

    fun onEndClicked(status: CallStatus) {
        _callStatus.value = status
        rtcClient?.endCall()
        stopVideoTrack()
        clearSurfaces()
        val endTime = System.currentTimeMillis()
        updateCallEndTime(endTime)
        rtcClient = null
        _isInCall.value = false
        isInitialized = false
        _isCallAcceptedPending.value = false
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
        setCallStatus(CallStatus.COMPLETED)
        socketRepository.sendMessageToSocket(
            CallMessageModel(
                "end_call", userName, target, status.name,null
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

    private var currentCallLogId: String? = null
    private fun saveCallLog(callLog: CallLog) {
        fireStore.collection(CALLLOG)
            .add(callLog)
            .addOnSuccessListener { documentReference ->
                currentCallLogId = documentReference.id
                Log.d("CallLog", "Document added with ID: $currentCallLogId")
            }
            .addOnFailureListener { e ->
                Log.w("CallLog", "Error adding document", e)
            }
    }

    private fun updateCallEndTime(endTime: Long) {
        currentCallLogId?.let { id ->
            fireStore.collection(CALLLOG).document(id)
                .update("endTime", endTime, "status", CallStatus.COMPLETED)
                .addOnSuccessListener {
                    Log.d("CallLog", "Document successfully updated!")
                }
                .addOnFailureListener { e ->
                    Log.w("CallLog", "Error updating document", e)
                }
        }
    }
    fun fetchCallLogs() {
        fireStore.collection(CALLLOG)
            .get()
            .addOnSuccessListener { documents ->
                val logs = documents.map { document ->
                    document.toObject(CallLog::class.java)
                }.filter {
                    it.target != userName
                }
                _callLogs.value = logs
            }
            .addOnFailureListener { e ->
                Log.w("CallLog", "Error fetching documents", e)
            }
    }
    fun deleteAllCallLogs() {
        fireStore.collection(CALLLOG)
            .get()
            .addOnSuccessListener { documents ->
                val batch = fireStore.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        _callLogs.value = emptyList()
                        Log.d("CallViewModel", "All call logs deleted successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CallViewModel", "Error deleting call logs", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CallViewModel", "Error fetching call logs for deletion", e)
            }
    }

    override fun onNewMessage(message: CallMessageModel) {
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
                    val status = when (message.data) {
                        "missed" -> CallStatus.MISSED
                        "rejected" -> CallStatus.REJECTED
                        else -> CallStatus.COMPLETED
                    }
                    onEndClicked(status)
                    Log.d("RTCC", "call_ended $message")
                }
            }
        }
    }
}
