package com.example.bands.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.Message
import com.example.bands.data.api.WeatherModel
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallStatus
import com.example.bands.di.CallViewModel
import com.example.bands.di.WeatherViewModel
import com.example.bands.utils.CommonImage
import com.example.bands.utils.WeatherShowText
import com.example.bands.utils.navigateTo
import com.example.bands.weatherupdates.NetworkResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun SingleChatScreen(
    navController: NavController,
    viewModel: BandsViewModel,
    callViewModel: CallViewModel,
    chatId: String,
    weatherViewModel: WeatherViewModel
) {
    val chatUserCityNameResult = weatherViewModel.chatUserCityName.observeAsState()
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
    //val chatMessages = viewModel.chatMessages
    val chatMessages by viewModel.chatMessages.collectAsState()
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.sendImageMessage(chatId, it)
                Toast.makeText(context, "Image selected!", Toast.LENGTH_SHORT).show()
            }
        }
    val onImageSelected: () -> Unit = {
        launcher.launch("image/*")
    }
    val showDeleteIcon = remember { mutableStateOf(false) }
    val selectedMessageId = remember { mutableStateOf<String?>(null) }

    //LaunchedEffect(Unit) { callViewModel.trackRoute(DestinationScreen.SingleChat.route) }

    LaunchedEffect(key1 = Unit) {
        callViewModel.setCallStatus(CallStatus.NOTINCALL)
        mainUser?.phoneNumber?.let {
            callViewModel.init(it)
        }
        viewModel.loadMessages(chatId)
        weatherViewModel.fetchWeatherDataFromDatabase(city = chatUser.city ?: "")
    }

    BackHandler {
        if (callViewModel.isInCall.value) {
            callViewModel.onEndClicked(CallStatus.COMPLETED)
        }
        navigateTo(navController, DestinationScreen.ChatList.route)
        viewModel.releaseMessages()
    }

    val chatUserWeatherData: WeatherModel? = when (val result = chatUserCityNameResult.value) {
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
        ChatHeader(
            name = chatUser.name ?: "",
            imageUrl = chatUser.imageUrl ?: "",
            chatUserWeatherData,
            onDeleteChat = {
                viewModel.deleteAllMessagesInChat(chatId)
            },
            onBacKClicked = {
                navController.popBackStack()
                viewModel.releaseMessages()
            },
            onStartVideoCallButtonClicked = {
                callViewModel.setCallStatus(CallStatus.ONGOING)
                chatUser.let {
                    navController.navigate(
                        DestinationScreen.CallScreen.createRoute(
                            it.name ?: "",
                            it.phoneNumber ?: "",
                            "false"
                        )
                    )
                    //it.phoneNumber?.let { it1 -> callViewModel.startCall(it1) }
                }
            },
            onStartAudioCallButtonClicked = {
                callViewModel.setCallStatus(CallStatus.ONGOING)
                chatUser.let {
                    navController.navigate(
                        DestinationScreen.CallScreen.createRoute(
                            it.name ?: "",
                            it.phoneNumber ?: "",
                            "true"
                        )
                    )
                }
            },
            showDeleteIcon = showDeleteIcon,
            selectedMessageId = selectedMessageId,
            onDeleteMessage = {
                if (it != null) {
                    viewModel.deleteMessage(chatId, it)
                }
            }
        )
        MessageBox(
            modifier = Modifier
                .weight(1f)
                .background(colorResource(id = R.color.chatBgColor)),
            chatId,
            chatMessages = chatMessages,
            currentUserId = mainUser?.userId ?: "",
            viewModel,
            showDeleteIcon,
            selectedMessageId
        )
        ReplyBox(
            reply = reply,
            onReplyChange = { reply = it },
            onSendReply = onSendReply,
            onSelectImage = onImageSelected
        )
    }
}

fun formatTimestamp(timestampString: String?): String {
    val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
    val timestamp = LocalDateTime.parse(timestampString, formatter)
    val displayFormatter = DateTimeFormatter.ofPattern("h:mm a")
    return timestamp.format(displayFormatter)
}

@Composable
fun EmojiPicker(
    modifier: Modifier = Modifier,
    onEmojiSelected: (String) -> Unit
) {
    val initialEmojis = listOf("👍🏻", "❤️", "😂", "😮", "😢", "😡", "😎")
    val allEmojis = listOf(
        "👍🏻", "❤️", "😂", "😮", "😢", "😎", "😡",
        "👏🏻", "👌🏻", "🤲🏻", "🙏🏻", "🤗", "🤩",
        "🔥", "✨", "💯", "🤘", "🚀", "🌈",
        "🌟", "🎉", "💥", "🥂", "🎶"
    )
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            Modifier
                .wrapContentSize()
                .align(Alignment.CenterHorizontally)
        ) {
            val emojisToShow = if (expanded) allEmojis else initialEmojis
            items(emojisToShow) { emoji ->
                Text(
                    text = emoji,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onEmojiSelected(emoji) },
                    fontSize = 22.sp
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "–" else "+",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 22.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBox(
    modifier: Modifier,
    chatId: String,
    chatMessages: List<Message>,
    currentUserId: String,
    viewModel: BandsViewModel,
    showDeleteIcon: MutableState<Boolean>,
    selectedMessageId: MutableState<String?>,
) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true
    ) {
        items(chatMessages.reversed()) { message ->
            val alignment = if (message.sendBy == currentUserId) Alignment.End else Alignment.Start
            val color1 = 0xFFB3D1EA
            val color2 = 0xFFFFCCAA
            val color = if (message.sendBy == currentUserId) Color(color1) else Color(color2)
            val formattedTime = formatTimestamp(message.timeStamp)
            var showEmojiPicker by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showEmojiPicker = false
                                showDeleteIcon.value = false
                            },
                            onLongPress = {
                                showEmojiPicker = !showEmojiPicker
                                selectedMessageId.value = message.id
                                showDeleteIcon.value = true
                            }
                        )
                    },
                horizontalAlignment = alignment
            ) {
                if (showEmojiPicker) {
                    EmojiPicker(
                        onEmojiSelected = { emoji ->
                            viewModel.addReactionToMessage(chatId, message.id, emoji)
                            showEmojiPicker = false
                        },
                        modifier = Modifier.align(alignment)
                    )
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(12.dp)
                ) {
                    if (message.imageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = message.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .height(200.dp)
                                .wrapContentSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Text(
                        text = message.message ?: "",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        //.padding(top = 4.dp)
                        .offset(y = (-14).dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(4.dp)
                ) {
                    message.reactions.forEach { reaction ->
                        Text(
                            text = reaction,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(4.dp),
                        )
                    }
                }
                Text(
                    text = formattedTime,
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    modifier = Modifier.align(alignment)
                )
            }
        }
    }
}


@Composable
fun ReplyBox(
    reply: String,
    onReplyChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onSelectImage: () -> Unit
) {
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
                ), trailingIcon = {
                    IconButton(onClick = onSelectImage) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = "add image"
                        )
                    }
                }
            )
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
fun ChatHeader(
    name: String,
    imageUrl: String,
    data: WeatherModel? = null,
    onDeleteChat: () -> Unit,
    onBacKClicked: () -> Unit,
    onStartVideoCallButtonClicked: () -> Unit,
    onStartAudioCallButtonClicked: () -> Unit,
    showDeleteIcon: MutableState<Boolean>,
    selectedMessageId: MutableState<String?>,
    onDeleteMessage: (String?) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val menuExpanded = remember { mutableStateOf(false) }
    val showDialog = remember { mutableStateOf(false) }
    val showDeleteMessageDialog = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 74.dp),
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
    ) {
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
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_startcall_24),
                        contentDescription = "audioCall",
                        Modifier
                            .clickable { onStartAudioCallButtonClicked.invoke() }
                            .size(32.dp)

                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_videocam_24),
                        contentDescription = "videoCall",
                        Modifier
                            .clickable { onStartVideoCallButtonClicked.invoke() }
                            .size(32.dp)

                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    if (showDeleteIcon.value) {
                        IconButton(onClick = {
                            showDeleteMessageDialog.value = true

                        }) {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete message"
                            )
                        }
                    }
                    IconButton(onClick = { menuExpanded.value = !menuExpanded.value }) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "clear Chat"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded.value,
                        onDismissRequest = { menuExpanded.value = false }) {
                        DropdownMenuItem(
                            onClick = {
                                showDialog.value = true
                                menuExpanded.value = false
                            },
                        ) {
                            Text(text = "Clear Chat", color = Color.White)
                        }
                    }
                }
            }
            if (showDialog.value) {
                ConfirmationDialog(
                    onConfirm = {
                        onDeleteChat()
                        showDialog.value = false
                    },
                    onDismiss = { showDialog.value = false }
                )
            }
            if (showDeleteMessageDialog.value) {
                ConfirmationDialogForDeleteMessage(
                    onConfirm = {
                        onDeleteMessage(selectedMessageId.value)
                        showDeleteMessageDialog.value = false
                        showDeleteIcon.value = false
                    },
                    onDismiss = {
                        showDeleteIcon.value = false
                        showDeleteMessageDialog.value = false
                    }
                )

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
fun ConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.5f),
            tonalElevation = 12.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delete all chats?",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        onConfirm()
                        onDismiss()
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialogForDeleteMessage(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.5f),
            tonalElevation = 12.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delete all chats?",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        onConfirm()
                        onDismiss()
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}




