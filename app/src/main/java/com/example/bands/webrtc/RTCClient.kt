package com.example.bands.webrtc

import android.app.Application
import android.util.Log
import com.example.bands.data.MessageModel
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
    private val userName:String,
    private val socketRepository: SocketRepository,
    private val observer :PeerConnection.Observer
) {


    init {
        initPeerConnectionFactory(application)
    }

    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }

//    private val iceServer = listOf(
//        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
//        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),
//        PeerConnection.IceServer("turn:openrelay.metered.ca:80","openrelayproject","openrelayproject"),
//        PeerConnection.IceServer("turn:openrelay.metered.ca:443","openrelayproject","openrelayproject"),
//        PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp","openrelayproject","openrelayproject"),
//        )
    private val iceServer= listOf(
        PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("9413fdb2cc64ea912deaa464")
            .setPassword("6aOOsT45M6b4AkH+")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("9413fdb2cc64ea912deaa464")
            .setPassword("6aOOsT45M6b4AkH+")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("9413fdb2cc64ea912deaa464")
            .setPassword("6aOOsT45M6b4AkH+")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
            .setUsername("9413fdb2cc64ea912deaa464")
            .setPassword("6aOOsT45M6b4AkH+")
            .createIceServer()
    )

    private val peerConnection by lazy { createPeerConnectionFactory(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private var videoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private fun initPeerConnectionFactory(application: Application){
        val peerConnectionOption=PeerConnectionFactory.InitializationOptions.builder(application)
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
    fun initSurfaceView(surfaceView: SurfaceViewRenderer){
        surfaceView.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)
        }
    }
    fun startLocalVideo(surfaceView: SurfaceViewRenderer){
        val surfaceTextureHelper=SurfaceTextureHelper.create(Thread.currentThread().name,eglContext.eglBaseContext)
        videoCapturer =getVideoCapturer(application)
        videoCapturer?.initialize(surfaceTextureHelper,surfaceView.context,localVideoSource.capturerObserver)
        videoCapturer?.startCapture(320,240,30)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
        localVideoTrack?.addSink(surfaceView)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_track_audio",localAudioSource)
        val localStream=peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
    }
    fun startLocalAudio(){
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }
    private fun getVideoCapturer(application: Application):CameraVideoCapturer{
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?: throw IllegalStateException()
        }
    }
    fun call(target:String){
        val mediaConstraints =MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo","true"))
        peerConnection?.createOffer(object :SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object :SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val offer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepository.sendMessageToSocket(
                            MessageModel(
                                "create_offer",userName,target,offer
                            )
                        )
                        Log.d("RTCC","create_offer $target")
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

        },mediaConstraints)
    }
    fun onRemoteSessionReceived(session: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {


            }

            override fun onSetSuccess() {
                Log.d("RTCC","onRemoteSessionReceived Success")

            }

            override fun onCreateFailure(p0: String?) {
                Log.d("RTCC","onRemoteSessionReceived Failure$p0")

            }

            override fun onSetFailure(p0: String?) {

            }

        }, session)

    }
    fun answer(target: String) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type

                        )
                        socketRepository.sendMessageToSocket(
                            MessageModel(
                                "create_answer",userName,target,answer
                            )
                        )
                        Log.d("RTCC","create_answer $target")
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

        }, constraints)

    }

    fun addIceCandidate(p0: IceCandidate?) {
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
    }


}