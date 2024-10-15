package com.example.bands.webrtc

import android.app.Application
import android.util.Log
import com.example.bands.data.MessageModel
import com.example.bands.socket.SocketRepository
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.Observer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class RTCClient(
    private val application: Application,
    private val username:String,
    private val socketRepository: SocketRepository,
    private val observer :PeerConnection.Observer
) {

    init {
        initPeerConnectionFactory(application)
    }
    private val eglContext=EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:80","openrelayproject","openrelayproject"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:443","openrelayproject","openrelayproject"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp","openrelayproject","openrelayproject"),)

    private val peerConnection by lazy { createPeerConnectionFactory(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private var videoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private fun initPeerConnectionFactory(application: Application) {
        val peerConnectionOption =PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }
    private fun createPeerConnectionFactory():PeerConnectionFactory{
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext.eglBaseContext,true,true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption=true
                disableNetworkMonitor=true
            }).createPeerConnectionFactory()
    }
    private fun createPeerConnectionFactory(observer: Observer):PeerConnection?{
        return peerConnectionFactory.createPeerConnection(iceServer,observer)
    }

    fun initSurfaceView(surface: SurfaceViewRenderer, isLocalView: Boolean) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(isLocalView)
            init(eglContext.eglBaseContext, null)
        }
    }
    fun startLocalVideo(surfaceView: SurfaceViewRenderer){
        val surfaceTextureHelper=SurfaceTextureHelper.create(Thread.currentThread().name,eglContext.eglBaseContext)
        videoCapturer = getVideoCapturer(application)
//        videoCapturer?.initialize(surfaceTextureHelper,surfaceView.context,localVideoSource.capturerObserver)
//        videoCapturer?.startCapture(320,240,30)
        videoCapturer?.initialize(surfaceTextureHelper, surfaceView.context, localVideoSource.capturerObserver)
        try {
            videoCapturer?.startCapture(320, 240, 30)
            Log.d("RTCClient", "Started local video capture")
        } catch (e: Exception) {
            Log.e("RTCClient", "Failed to start local video capture: ${e.message}")
        }
        localVideoTrack =peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
        localVideoTrack?.addSink(surfaceView)
        localAudioTrack =peerConnectionFactory.createAudioTrack("local_track_audio",localAudioSource)
        val localStream=peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)

    }

    private fun getVideoCapturer(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?: throw IllegalStateException()
        }
    }
    fun call(target:String){
        val mediaConstraints=MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo","true"))
        peerConnection?.createOffer(object :SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d("CallViewModel", "Offer created successfully: ${desc?.description}")
                peerConnection?.setLocalDescription(object :SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val offer= hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepository.sendMessageToSocket(
                            MessageModel(
                                "create_offer",username,target,offer
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.e("CallViewModel", "Failed to set local description: $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.e("CallViewModel", "Failed to set local description: $p0")
                    }

                },desc)

            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {

            }

        },mediaConstraints)
    }
    fun onRemoteSessionReceived(session:SessionDescription){
        peerConnection?.setRemoteDescription(object :SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {
            }
            override fun onSetSuccess() {
                Log.d("RTCClient", "Remote session description set successfully")
            }
            override fun onCreateFailure(p0: String?) {
            }
            override fun onSetFailure(p0: String?) {
                Log.e("RTCClient", "Failed to set remote session description: $p0")
            }

        },session)
    }

    fun answer(target: String){
        val constraints=MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo","true"))
        peerConnection?.createAnswer(object :SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
              peerConnection?.setLocalDescription(object :SdpObserver{
                  override fun onCreateSuccess(p0: SessionDescription?) {

                  }

                  override fun onSetSuccess() {
                      val answer= hashMapOf(
                          "sdp" to desc?.description,
                          "type" to desc?.type
                      )
                      socketRepository.sendMessageToSocket(
                          MessageModel(
                          "create_answer",username,target,answer
                      )
                      )

                  }

                  override fun onCreateFailure(p0: String?) {
                  }

                  override fun onSetFailure(p0: String?) {

                  }

              },desc)
            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {

            }

            override fun onSetFailure(p0: String?) {

            }

        },constraints)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        Log.d("RTCClient", "Adding ICE candidate: ${p0?.sdp}")
        peerConnection?.addIceCandidate(p0)
    }
    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(mute)
    }

    fun toggleCamera(cameraPause: Boolean) {
        localVideoTrack?.setEnabled(cameraPause)
    }

    fun endCall() {
        peerConnection?.close()
        videoCapturer?.stopCapture()
        videoCapturer = null
    }

}