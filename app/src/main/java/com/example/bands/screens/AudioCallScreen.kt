package com.example.bands.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bands.R
import com.example.bands.di.CallViewModel
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

//@Composable
//fun AudioCallScreen(callViewModel: CallViewModel, receiverName: String) {
//    var callDuration by remember { mutableLongStateOf(0L) }
//    val isCallActive =false
//
//    LaunchedEffect(isCallActive) {
//        if (isCallActive) {
//            callDuration = 0L
//            val startTime = System.currentTimeMillis()
//            while (isCallActive) {
//                callDuration = (System.currentTimeMillis() - startTime) / 1000
//                delay(1000)
//            }
//        }
//    }
//    DisposableEffect(Unit) {
//        onDispose {
//            callDuration = 0L
//        }
//    }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = MaterialTheme.colorScheme.background
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(24.dp),
//            verticalArrangement = Arrangement.SpaceBetween,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Box(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    PulsingAnimation(receiverName)
//
//                    Text(
//                        text = formatDuration(callDuration),
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(top = 16.dp)
//                    )
//                }
//            }
//            ControlButtonsLayout(
//                modifier = Modifier.height(88.dp).fillMaxWidth(),
//                onAudioButtonClicked = callViewModel::audioButtonClicked,
//                onCameraButtonClicked = {},
//                onEndCallClicked = callViewModel::onEndCallClicked,
//                onSwitchCameraClicked = {},
//                isAudioCall = true
//            )
//        }
//    }
//}
//
//@Composable
//fun PulsingAnimation(receiverName: String) {
//    val infiniteTransition = rememberInfiniteTransition(label = "")
//    val scale by infiniteTransition.animateFloat(
//        initialValue = 0.8f,
//        targetValue = 1.2f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(1000, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        ), label = ""
//    )
//
//    Text(
//        text = receiverName,
//        fontSize = 28.sp,
//        fontWeight = FontWeight.Bold,
//        color = Color.Black,
//        modifier = Modifier.scale(scale)
//    )
//}
//
//fun formatDuration(seconds: Long): String {
//    val minutes = seconds / 60
//    val remainingSeconds = seconds % 60
//    return String.format("%02d:%02d", minutes, remainingSeconds)
//}
//@Composable
//fun ControlButtonsLayout(
//    modifier: Modifier,
//    onAudioButtonClicked: (Boolean) -> Unit,
//    onCameraButtonClicked: (Boolean) -> Unit,
//    onEndCallClicked: () -> Unit,
//    onSwitchCameraClicked: () -> Unit,
//    isAudioCall: Boolean
//) {
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .background(
//                color = Color(0x80FFFFFF),
//                shape = RoundedCornerShape(16.dp)
//            )
//            .padding(12.dp),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceEvenly
//    ) {
//
//        val audioState = remember { mutableStateOf(true) }
//        LaunchedEffect(key1 = audioState.value, block = {
//            onAudioButtonClicked.invoke(audioState.value)
//        })
//
//        IconButton(onClick = {
//            audioState.value = !audioState.value
//        }) {
//            Icon(
//                painter = if (audioState.value) painterResource(id = R.drawable.baseline_mic_24) else painterResource(id = R.drawable.baseline_mic_off_24),
//                contentDescription = "Toggle Audio",
//                tint = if (audioState.value) Color.Black else Color.Red
//            )
//        }
//
//        val cameraSate = remember { mutableStateOf(true) }
//        LaunchedEffect(key1 = cameraSate.value, block = {
//            onCameraButtonClicked.invoke(cameraSate.value)
//        })
//        if (!isAudioCall) {
//            IconButton(onClick = {
//                cameraSate.value = !cameraSate.value
//            }) {
//                Icon(
//                    painter = if (cameraSate.value) painterResource(id = R.drawable.baseline_videocam_24) else painterResource(
//                        id = R.drawable.baseline_videocam_off_24
//                    ),
//                    contentDescription = "Toggle Video",
//                    tint = if (cameraSate.value) Color.Black else Color.Red
//                )
//            }
//        }
//
//        IconButton(onClick = { onEndCallClicked.invoke()
//        }) {
//            Icon(
//                painter = painterResource(id = R.drawable.baseline_call_end_24),
//                contentDescription = "End Call"
//            )
//        }
//
//        if (!isAudioCall) {
//            IconButton(onClick = { onSwitchCameraClicked.invoke() }) {
//                Icon(
//                    painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
//                    contentDescription = "Switch Camera"
//                )
//            }
//        }
//    }
//}
//@Composable
//fun MainVideoCallUI(callViewModel: CallViewModel) {
//    var isCallVisible by remember { mutableStateOf(true) }
//    val audioState by remember { mutableStateOf(true) }
//    val cameraSate by remember { mutableStateOf(true) }
//
//    VideoCallScreen(
//        isVisible = isCallVisible,
//        callViewModel,
//        onMicToggle = { callViewModel.audioButtonClicked(audioState) },
//        onVideoToggle = { callViewModel.videoButtonClicked(cameraSate) },
//        onEndCall = {
//            isCallVisible = false
//            callViewModel.onEndClicked()
//        },
//        onSwitchCamera = { callViewModel.cameraSwitchClicked() },
//        onAudioOutputToggle = { },
//        audioState,
//        cameraSate
//    )
//}
//
//@Composable
//fun MainAudioCallUI(callViewModel: CallViewModel) {
//    var isCallVisible by remember { mutableStateOf(true) }
//    val callerName = "Someone calling"
//    val audioState by remember { mutableStateOf(true) }
//
//    AudioCallScreen(
//        isVisible = isCallVisible,
//        callerName = callerName,
//        onMicToggle = { callViewModel.audioButtonClicked(audioState) },
//        onEndCall = {
//            isCallVisible = false
//            callViewModel.onEndClicked()
//        },
//        onAudioOutputToggle = { /* Handle audio output toggle */ },
//        audioState
//    )
//}
//
//@Composable
//fun VideoCallScreen(
//    isVisible: Boolean,
//    callViewModel: CallViewModel,
//    onMicToggle: () -> Unit,
//    onVideoToggle: () -> Unit,
//    onEndCall: () -> Unit,
//    onSwitchCamera: () -> Unit,
//    onAudioOutputToggle: () -> Unit,
//    audioState: Boolean,
//    cameraSate: Boolean
//) {
//    if (isVisible) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//        ) {
//            // Remote Video View (Fullscreen)
//            AndroidView(
//                factory = { context ->
//                    SurfaceViewRenderer(context).apply {
//                        callViewModel.setRemoteSurface(this)
//                    }
//                },
//                modifier = Modifier.fillMaxSize()
//            )
//
//            // Local Video View (Small Preview)
//            AndroidView(
//                factory = { context ->
//                    SurfaceViewRenderer(context).apply {
//                        // Initialize renderer as needed
//                        callViewModel.setLocalSurface(this)
//                    }
//                },
//                modifier = Modifier
//                    .size(width = 120.dp, height = 150.dp)
//                    .align(Alignment.TopEnd)
//                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
//            )
//
//            // Loading Indicator (ProgressBar)
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//
//            // Controls at the Bottom
//            Column(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .fillMaxWidth()
//                    .background(androidx.compose.material.MaterialTheme.colors.onSecondary.copy(alpha = 0.5f))
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Row(
//                    horizontalArrangement = Arrangement.SpaceAround,
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    IconButton(
//                        onClick = onMicToggle,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = if (audioState) painterResource(id = R.drawable.baseline_mic_24) else painterResource(
//                                id = R.drawable.baseline_mic_off_24
//                            ),
//                            contentDescription = "Toggle Audio",
//                            tint = if (audioState) Color.Black else Color.Red
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onVideoToggle,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = if (cameraSate) painterResource(id = R.drawable.baseline_videocam_24) else painterResource(
//                                id = R.drawable.baseline_videocam_off_24
//                            ),
//                            contentDescription = "Toggle Video",
//                            tint = if (cameraSate) Color.Black else Color.Red
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onEndCall,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Red,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.baseline_call_end_24),
//                            contentDescription = "End Call"
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onSwitchCamera,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
//                            contentDescription = "Switch Camera"
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onAudioOutputToggle,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
//                            contentDescription = "Switch Camera"
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//@Composable
//fun AudioCallScreen(
//    isVisible: Boolean,
//    callerName: String,
//    onMicToggle: () -> Unit,
//    onEndCall: () -> Unit,
//    onAudioOutputToggle: () -> Unit,
//    audioState: Boolean
//) {
//    if (isVisible) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.White)
//        ) {
//            // Caller Name
//            Text(
//                text = callerName,
//                fontSize = 20.sp,
//                color = Color.Black,
//                modifier = Modifier.align(Alignment.Center)
//            )
//
//            // Loading Indicator (ProgressBar)
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//
//            // Controls at the Bottom
//            Column(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .fillMaxWidth()
//                    .background(androidx.compose.material.MaterialTheme.colors.onSecondary.copy(alpha = 0.5f))
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Row(
//                    horizontalArrangement = Arrangement.SpaceAround,
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    IconButton(
//                        onClick = onMicToggle,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = if (audioState) painterResource(id = R.drawable.baseline_mic_24) else painterResource(
//                                id = R.drawable.baseline_mic_off_24
//                            ),
//                            contentDescription = "Toggle Audio",
//                            tint = if (audioState) Color.Black else Color.Red
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onEndCall,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Red,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.baseline_call_end_24),
//                            contentDescription = "End Call"
//                        )
//                    }
//
//                    IconButton(
//                        onClick = onAudioOutputToggle,
//                        modifier = Modifier
//                            .background(
//                                color = Color.Black,
//                                shape = CircleShape
//                            )
//                            .padding(12.dp)
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
//                            contentDescription = "Switch Camera"
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

