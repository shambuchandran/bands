package com.example.bands.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.Message
import com.example.bands.data.api.WeatherModel
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallViewModel
import com.example.bands.di.WeatherViewModel
import com.example.bands.utils.CommonImage
import com.example.bands.utils.WeatherShowText
import com.example.bands.utils.navigateTo
import com.example.bands.weatherupdates.NetworkResponse
import org.webrtc.SurfaceViewRenderer


@Composable
fun SingleChatScreen(
    navController: NavController,
    viewModel: BandsViewModel,
    callViewModel: CallViewModel,
    chatId: String,
    weatherViewModel:WeatherViewModel
) {
    val chatUserCityNameResult=weatherViewModel.chatUserCityName.observeAsState()
    var reply by rememberSaveable { mutableStateOf("") }
    val onSendReply = {
        if (reply.isNotBlank()) {
            viewModel.onSendReply(chatId, reply)
            reply = ""
        }
    }
    val mainUser = viewModel.userData.value
    val currentChat = viewModel.chats.value.first { it.chatId == chatId }
    val chatUser =
        if (mainUser?.userId == currentChat.user1.userId) currentChat.user2 else currentChat.user1
    val chatMessages = viewModel.chatMessages
    val isAudioCallUi =callViewModel.isAudioCall.collectAsState()
    val isInCall = callViewModel.isInCall.collectAsState()



    LaunchedEffect(key1 = Unit) {
        viewModel.loadMessages(chatId)
        weatherViewModel.fetchWeatherDataFromDatabase(city = chatUser.city?:"")
    }

    BackHandler {
        if (callViewModel.isInCall.value) {
            callViewModel.onEndClicked()
        }
        navigateTo(navController, DestinationScreen.ChatList.route)
        viewModel.releaseMessages()
    }
    val chatUserWeatherData: WeatherModel? = when (val result = chatUserCityNameResult.value)
    {
        is NetworkResponse.Error -> {
            null
        }
        NetworkResponse.Loading -> {
            null
        }
        is NetworkResponse.Success -> {
            result.data
        }
        null -> {
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isInCall.value) {
            if (isAudioCallUi.value) {
                // Show Audio Call Screen
                MainAudioCallUI(callViewModel)
            } else {
                // Show Video Call Screen
                MainVideoCallUI(callViewModel)
            }
        } else {

            ChatHeader(
                name = chatUser.name ?: "",
                imageUrl = chatUser.imageUrl ?: "",
                chatUserWeatherData,
                onBacKClicked = {
                    navController.popBackStack()
                    viewModel.releaseMessages()
                },
                onStartCallButtonClicked = {
                    chatUser.phoneNumber?.let { callViewModel.startVideoCall(it) }

                },
                onStartAudioCallButtonClicked = {
                    chatUser.phoneNumber?.let { callViewModel.startAudioCall(it) }
                }
            )
            MessageBox(
                modifier = Modifier
                    .weight(1f)
                    .background(colorResource(id = R.color.chatBgColor)),
                chatMessages = chatMessages.value,
                currentUserId = mainUser?.userId ?: ""
            )
            ReplyBox(reply = reply, onReplyChange = { reply = it }, onSendReply = onSendReply)
        }
    }
}



@Composable
fun MessageBox(modifier: Modifier, chatMessages: List<Message>, currentUserId: String) {
    LazyColumn(modifier) {
        items(chatMessages) { Message ->
            val alignment = if (Message.sendBy == currentUserId) Alignment.End else Alignment.Start
            val color1=0xFFB3D1EA //0xFFB3D1EA ,0xFFA8D5BA
            val color2=0xFFFFCCAA //0xFFFFCCAA ,0xFFFFB3B3
            val color = if (Message.sendBy == currentUserId) Color(color1) else Color(color2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = alignment
            ) {
                Text(
                    text = Message.message ?: "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ReplyBox(reply: String, onReplyChange: (String) -> Unit, onSendReply: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colorResource(id = R.color.BgColor))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TextField for input
            TextField(
                value = reply,
                onValueChange = onReplyChange,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFD1C7B9), //0xFFD1C7B9
                    unfocusedContainerColor = Color(0xFFF6F0E7), //0xFFF6F0E7
                    disabledContainerColor = Color(0xFFD8CFC4), //0xFFD8CFC4
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            // Circular Send Button
            IconButton(
                onClick = onSendReply,
                modifier = Modifier
                    .size(52.dp)
                    .background(colorResource(id = R.color.BgColor), shape = CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun ChatHeader(name: String, imageUrl: String,data: WeatherModel? = null, onBacKClicked: () -> Unit,onStartCallButtonClicked:() -> Unit,onStartAudioCallButtonClicked: () -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 74.dp)
        ,
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
    ){
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    //.height(88.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(colorResource(id = R.color.BgColor)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "back",
                        Modifier
                            .clickable { onBacKClicked.invoke() }
                            .padding(4.dp)
                    )
                    CommonImage(
                        data = imageUrl,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }


                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Call,
                        contentDescription = "audioCall",
                        Modifier
                            .clickable { onStartAudioCallButtonClicked.invoke() }
                            .size(50.dp)
                            .padding(end = 8.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_videocam_24),
                        contentDescription = "videoCall",
                        Modifier
                            .clickable { onStartCallButtonClicked.invoke() }
                            .size(50.dp)
                            .padding(end = 8.dp)
                    )
                }
            }
            if (data != null) {
                IconButton(
                    onClick = { expanded.value = !expanded.value },
                    modifier = Modifier
                        .offset(x = (-8).dp, y = (-8).dp)
                        .size(20.dp)
                        .align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse"
                    )
                }
            }
            if (expanded.value && data != null) {
                WeatherShowText(data)
            }
        }
    }
}

@Composable
fun MainVideoCallUI(callViewModel: CallViewModel) {
    var isCallVisible by remember { mutableStateOf(true) }
    val audioState by remember { mutableStateOf(true) }
    val cameraSate by remember { mutableStateOf(true) }

    VideoCallScreen(
        isVisible = isCallVisible,
        callViewModel,
        onMicToggle = { callViewModel.audioButtonClicked(audioState) },
        onVideoToggle = { callViewModel.videoButtonClicked(cameraSate) },
        onEndCall = { isCallVisible = false
                    callViewModel.onEndClicked()},
        onSwitchCamera = { callViewModel.cameraSwitchClicked() },
        onAudioOutputToggle = { },
        audioState,
        cameraSate
    )
}
@Composable
fun MainAudioCallUI(callViewModel: CallViewModel) {
    var isCallVisible by remember { mutableStateOf(true) }
    val callerName = "Someone calling"
    val audioState by remember { mutableStateOf(true) }

    AudioCallScreen(
        isVisible = isCallVisible,
        callerName = callerName,
        onMicToggle = { callViewModel.audioButtonClicked(audioState) },
        onEndCall = { isCallVisible = false
            callViewModel.onEndClicked()},
        onAudioOutputToggle = { /* Handle audio output toggle */ },
        audioState
    )
}
@Composable
fun VideoCallScreen(
    isVisible: Boolean,
    callViewModel: CallViewModel,
    onMicToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onEndCall: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAudioOutputToggle: () -> Unit,
    audioState:Boolean,
    cameraSate:Boolean
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Remote Video View (Fullscreen)
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        callViewModel.setRemoteSurface(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Local Video View (Small Preview)
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).apply {
                        // Initialize renderer as needed
                        callViewModel.setLocalSurface(this)
                    }
                },
                modifier = Modifier
                    .size(width = 120.dp, height = 150.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
            )

            // Loading Indicator (ProgressBar)
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            // Controls at the Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.onSecondary.copy(alpha = 0.5f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onMicToggle,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = if (audioState) painterResource(id = R.drawable.baseline_mic_24) else painterResource(id = R.drawable.baseline_mic_off_24),
                            contentDescription = "Toggle Audio",
                            tint = if (audioState) Color.Black else Color.Red
                        )
                    }

                    IconButton(
                        onClick = onVideoToggle,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = if (cameraSate) painterResource(id = R.drawable.baseline_videocam_24) else painterResource(
                                id = R.drawable.baseline_videocam_off_24
                            ),
                            contentDescription = "Toggle Video",
                            tint = if (cameraSate) Color.Black else Color.Red
                        )
                    }

                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .background(
                                color = Color.Red,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_call_end_24),
                            contentDescription = "End Call"
                        )
                    }

                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                            contentDescription = "Switch Camera"
                        )
                    }

                    IconButton(
                        onClick = onAudioOutputToggle,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                                contentDescription = "Switch Camera"
                            )
                    }
                }
            }
        }
    }
}


@Composable
fun AudioCallScreen(
    isVisible: Boolean,
    callerName: String,
    onMicToggle: () -> Unit,
    onEndCall: () -> Unit,
    onAudioOutputToggle: () -> Unit,
    audioState:Boolean
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Caller Name
            Text(
                text = callerName,
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )

            // Loading Indicator (ProgressBar)
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            // Controls at the Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.onSecondary.copy(alpha = 0.5f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onMicToggle,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = if (audioState) painterResource(id = R.drawable.baseline_mic_24) else painterResource(id = R.drawable.baseline_mic_off_24),
                            contentDescription = "Toggle Audio",
                            tint = if (audioState) Color.Black else Color.Red
                        )
                    }

                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .background(
                                color = Color.Red,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_call_end_24),
                            contentDescription = "End Call"
                        )
                    }

                    IconButton(
                        onClick = onAudioOutputToggle,
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                            contentDescription = "Switch Camera"
                        )
                    }
                }
            }
        }
    }
}



