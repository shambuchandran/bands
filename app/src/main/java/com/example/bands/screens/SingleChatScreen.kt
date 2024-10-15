package com.example.bands.screens

import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.Message
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallViewModel
import com.example.bands.utils.CommonDivider
import com.example.bands.utils.CommonImage
import com.example.bands.utils.navigateTo
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer


@Composable
fun SingleChatScreen(
    navController: NavController,
    viewModel: BandsViewModel,
    callViewModel: CallViewModel,
    chatId: String
) {
    var reply by rememberSaveable { mutableStateOf("") }
    val onSendReply = {
        viewModel.onSendReply(chatId, reply)
        reply = ""
    }
    val mainUser = viewModel.userData.value
    val currentChat = viewModel.chats.value.first { it.chatId == chatId }

    val chatUser = if (mainUser?.userId == currentChat.user1.userId) currentChat.user2 else currentChat.user1
    val chatMessages = viewModel.chatMessages
    val isInCall = callViewModel.isInCall.collectAsState()


    LaunchedEffect(key1 = Unit) {
        viewModel.loadMessages(chatId)
    }
    BackHandler {
        if (callViewModel.isInCall.value) {
            callViewModel.onEndClicked()
        }
        navigateTo(navController, DestinationScreen.ChatList.route)
        viewModel.releaseMessages()
    }

    Column {
        if (isInCall.value) {
            CallView(callViewModel = callViewModel)
        } else {
            ChatHeader(
                name = chatUser.name ?: "",
                imageUrl = chatUser.imageUrl ?: "",
                onBacKClicked = {
                    if (callViewModel.isInCall.value) {
                        callViewModel.onEndClicked()
                    }
                    navController.popBackStack()
                    viewModel.releaseMessages()
                },
                onStartCallButtonClicked = {
                    callViewModel.startCall(chatUser.phoneNumber!!)
                    Log.d("chatUser.phoneNumber",chatUser.phoneNumber)
                })
            MessageBox(
                modifier = Modifier.weight(1f),
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
            val color =
                if (Message.sendBy == currentUserId) Color(0xff68c400) else Color(0xffffc0c0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = alignment
            ) {
                Text(
                    text = Message.message ?: "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
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
        Modifier.fillMaxWidth()
    ) {
        CommonDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = reply,
                onValueChange = onReplyChange,
                maxLines = 3,
                shape = RoundedCornerShape(8.dp)
            )
            Button(onClick = onSendReply) {
                Text(text = "Send")
            }
        }

    }
}

@Composable
fun ChatHeader(name: String, imageUrl: String, onBacKClicked: () -> Unit,onStartCallButtonClicked:() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .wrapContentHeight(),
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
                    .clickable { }
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


}

@Composable
fun CallView(callViewModel: CallViewModel) {
    var localSurfaceViewRenderer: SurfaceViewRenderer? by remember { mutableStateOf(null) }
    var remoteSurfaceViewRenderer: SurfaceViewRenderer? by remember { mutableStateOf(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            SurfaceViewRendererComposable(
                modifier = Modifier.weight(5f),
                onSurfaceReady = { remote ->
                    Log.d("CallView", "Remote Surface Initialized: $remote")
                    remoteSurfaceViewRenderer = remote
                    callViewModel.setRemoteSurface(remote)
                })
            Spacer(
                modifier = Modifier
                    .height(5.dp)
                    .background(color = Color.Gray)
            )
            SurfaceViewRendererComposable(
                modifier = Modifier.weight(5f),
                onSurfaceReady = { local ->
                    localSurfaceViewRenderer = local
                    callViewModel.setLocalSurface(local)
                    callViewModel.startLocalVideo()
                })
            if (localSurfaceViewRenderer != null && remoteSurfaceViewRenderer != null) {
                ControlButtonsLayout(
                    modifier = Modifier.weight(1f),
                    onAudioButtonClicked = callViewModel::audioButtonClicked,
                    onCameraButtonClicked = callViewModel::videoButtonClicked,
                    onEndCallClicked = callViewModel::onEndClicked,
                    onSwitchCameraClicked = callViewModel::cameraSwitchClicked
                )
            } else {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}


@Composable
fun SurfaceViewRendererComposable(
    modifier: Modifier,
    onSurfaceReady:(SurfaceViewRenderer)->Unit
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context  ->
            FrameLayout(context).apply {
                addView(SurfaceViewRenderer(context).also {
                    onSurfaceReady.invoke(it)
                })
            }
        }
    )
}
@Composable
fun ControlButtonsLayout(
    modifier: Modifier,
    onAudioButtonClicked: (Boolean) -> Unit,
    onCameraButtonClicked: (Boolean) -> Unit,
    onEndCallClicked: () -> Unit,
    onSwitchCameraClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        val audioState = remember { mutableStateOf(true) }
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

        val cameraSate = remember { mutableStateOf(true) }
        LaunchedEffect(key1 = cameraSate.value, block = {
            onCameraButtonClicked.invoke(cameraSate.value)
        })
        IconButton(onClick = {
            cameraSate.value = !cameraSate.value
        }) {
            Icon(
                painter = if (cameraSate.value) painterResource(id = R.drawable.baseline_videocam_24) else painterResource(id = R.drawable.baseline_videocam_off_24),
                contentDescription = "Toggle Video",
                tint = if (cameraSate.value) Color.Black else Color.Red
            )
        }

        IconButton(onClick = { onEndCallClicked.invoke()
        }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_call_end_24),
                contentDescription = "End Call"
            )
        }

        IconButton(onClick = { onSwitchCameraClicked.invoke() }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                contentDescription = "Switch Camera",
                Modifier.background(Color.LightGray)
            )
        }
    }
}
