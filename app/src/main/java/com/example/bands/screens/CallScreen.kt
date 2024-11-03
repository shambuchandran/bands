package com.example.bands.screens

import android.annotation.SuppressLint
import android.transition.Visibility
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.di.CallViewModel
import com.example.bands.utils.navigateTo
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@SuppressLint("SuspiciousIndentation")
@Composable
fun CallScreen(name:String,phoneNumber: String?,isAudioCall:Boolean,callViewModel: CallViewModel,navController: NavController) {
    Log.d("RTCC callscreen" ,"$isAudioCall")
    val context = LocalContext.current
    val localSurfaceViewRenderer = remember { SurfaceViewRenderer(context)}
    val remoteSurfaceViewRenderer = remember { SurfaceViewRenderer(context)}
    val isCallAcceptedPending by callViewModel.isCallAcceptedPending.collectAsState()

        LaunchedEffect(Unit) {
            callViewModel.setRemoteSurface(remoteSurfaceViewRenderer)
        callViewModel.rtcClient?.let { rtcClient ->
            rtcClient.initSurfaceView(localSurfaceViewRenderer)
            rtcClient.initSurfaceView(remoteSurfaceViewRenderer)
            rtcClient.startLocalVideo(localSurfaceViewRenderer)
        }
            delay(500)
            if (phoneNumber != null) {
                callViewModel.startCall(phoneNumber,isAudioCall)
            }
            if (isCallAcceptedPending) {
                callViewModel.acceptCallIfPending()
            }


    }
    DisposableEffect(Unit) {
        onDispose {
            callViewModel.onEndClicked()
            callViewModel.rtcClient = null
            localSurfaceViewRenderer.release()
            remoteSurfaceViewRenderer.release()
        }
    }
    BackHandler {
        if (callViewModel.isInCall.value) {
            callViewModel.onEndClicked()
            callViewModel.rtcClient = null
        }
        navController.popBackStack()
    }
    //MainVideoCallUI(callViewModel,navController,localSurfaceViewRenderer,remoteSurfaceViewRenderer)
    if (isAudioCall) {
        AudioCallScreen(callViewModel,name,navController)
    } else {
        MainVideoCallUI(callViewModel,navController,localSurfaceViewRenderer,remoteSurfaceViewRenderer)
    }
}
@Composable
fun AudioCallScreen(callViewModel: CallViewModel, receiverName: String,navController: NavController) {
    var callDuration by remember { mutableLongStateOf(0L) }
    val isCallActive =false

    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            callDuration = 0L
            val startTime = System.currentTimeMillis()
            while (isCallActive) {
                callDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            callDuration = 0L
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PulsingAnimation(receiverName)

                    Text(
                        text = formatDuration(callDuration),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            ControlButtonsLayout(
                modifier = Modifier
                    .height(88.dp)
                    .fillMaxWidth(),
                onAudioButtonClicked = callViewModel::audioButtonClicked,
                onCameraButtonClicked = {},
                onEndCallClicked = {callViewModel.onEndClicked()
                    navController.popBackStack()},
                onSwitchCameraClicked = {},
                isAudioCall = true
            )
        }
    }
}
@Composable
fun PulsingAnimation(receiverName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Text(
        text = receiverName,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.scale(scale)
    )
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
@Composable
fun ControlButtonsLayout(
    modifier: Modifier,
    onAudioButtonClicked: (Boolean) -> Unit,
    onCameraButtonClicked: (Boolean) -> Unit,
    onEndCallClicked: () -> Unit,
    onSwitchCameraClicked: () -> Unit,
    isAudioCall: Boolean
) {
    val audioState = remember { mutableStateOf(true) }
    val cameraSate = remember { mutableStateOf(true) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0x80FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {


        LaunchedEffect(key1 = audioState.value, block = {
            onAudioButtonClicked.invoke(audioState.value)
        })

        IconButton(onClick = {
            audioState.value = !audioState.value
        }) {
            Icon(
                painter = if (audioState.value) painterResource(id = R.drawable.baseline_mic_24) else painterResource(id = R.drawable.baseline_mic_off_24),
                contentDescription = "Toggle Audio",
                tint = if (audioState.value) Color.Black else Color.Red
            )
        }


        LaunchedEffect(key1 = cameraSate.value, block = {
            onCameraButtonClicked.invoke(cameraSate.value)
        })
        if (!isAudioCall) {
            IconButton(onClick = {
                cameraSate.value = !cameraSate.value
            }) {
                Icon(
                    painter = if (cameraSate.value) painterResource(id = R.drawable.baseline_videocam_24) else painterResource(
                        id = R.drawable.baseline_videocam_off_24
                    ),
                    contentDescription = "Toggle Video",
                    tint = if (cameraSate.value) Color.Black else Color.Red
                )
            }
        }

        IconButton(onClick = { onEndCallClicked.invoke()
        }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_call_end_24),
                contentDescription = "End Call"
            )
        }

        if (!isAudioCall) {
            IconButton(onClick = { onSwitchCameraClicked.invoke() }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                    contentDescription = "Switch Camera"
                )
            }
        }
    }
}

@Composable
fun MainVideoCallUI(callViewModel: CallViewModel,navController: NavController,localViewRenderer: SurfaceViewRenderer,remoteViewRenderer: SurfaceViewRenderer) {
//    var isCallVisible by remember { mutableStateOf(true) }
    VideoCallScreen(
        //isVisible = isCallVisible,
        callViewModel,
        onEndCall = {
            navController.popBackStack()
            callViewModel.onEndClicked()
            //isCallVisible = false
        },
        onSwitchCamera = { callViewModel.cameraSwitchClicked() },
        localViewRenderer,
        remoteViewRenderer
    )
}

@Composable
fun VideoCallScreen(
    callViewModel: CallViewModel,
    onEndCall: () -> Unit,
    onSwitchCamera: () -> Unit,
    localViewRenderer: SurfaceViewRenderer,
    remoteViewRenderer: SurfaceViewRenderer
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            addView(remoteViewRenderer)
                           //callViewModel.setRemoteSurface(remoteViewRenderer)
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            addView(localViewRenderer)
                            callViewModel.setLocalSurface(localViewRenderer)
                        }
                    }
                )
            }
        }
        ControlButtonsLayout(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            onAudioButtonClicked = { audioState ->
                callViewModel.audioButtonClicked(audioState)
            },
            onCameraButtonClicked = { cameraState ->
                callViewModel.videoButtonClicked(cameraState)
            },
            onEndCallClicked = onEndCall,
            onSwitchCameraClicked = onSwitchCamera,
            isAudioCall = false
        )
    }
}