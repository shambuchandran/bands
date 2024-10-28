package com.example.bands.screens

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bands.R
import com.example.bands.data.GemMessageModel
import com.example.bands.di.GemBotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GemChatPage(modifier: Modifier = Modifier, viewModel: GemBotViewModel,navController: NavController,context: Context) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.chatBgColor)),
        bottomBar = {
            MessageInput { message ->
                viewModel.sendMessage(message, context)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AppHeader(navController, onClearChat = { viewModel.clearChat() })
            MessageList(
                modifier = Modifier
                    .weight(1f)
                    .background(colorResource(id = R.color.chatBgColor))
                    .padding(bottom = 48.dp),
                messageList = viewModel.messageList
            )
        }
    }
}
fun formatTimestamp(timeStamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeStamp))
}

@Composable
fun MessageList(modifier: Modifier = Modifier, messageList: List<GemMessageModel>) {
    if (messageList.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.chatBgColor)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
//            Icon(
//                imageVector = Icons.Rounded.Search,
//                contentDescription = "Search",
//                modifier = Modifier.size(128.dp),
//                tint = Color.DarkGray
//            )
//            Text(text = "Ask GemBot", fontSize = 24.sp)
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.gem_animation))
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(400.dp)
                )
            } else {
                CircularProgressIndicator()
            }
            Text(text = "No messages yet. Ask GemBot!", fontSize = 20.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = modifier.background(colorResource(id = R.color.chatBgColor)),
            reverseLayout = true
        ) {
            items(messageList.reversed()) { message ->
                MessageRow(messageModel = message)
            }
        }
    }
}

@Composable
fun MessageRow(messageModel: GemMessageModel) {
    val isModel = messageModel.role == "model"
    val backgroundColor = if (isModel) Color(0xFF004D40) else Color(0xFFE0F7FA)
    val textColor = if (isModel) Color.White else Color.Black
    val formattedTime = formatTimestamp(messageModel.timeStamp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(if (isModel) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                Column {
                    SelectionContainer {
                        Text(
                            text = messageModel.message,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInput(onMessageSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(colorResource(id = R.color.chatBgColor)),
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.BgColor))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (message.isNotBlank()) {
                    onMessageSend.invoke(message)
                    message = ""
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(navController: NavController, onClearChat: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(colorResource(id = R.color.chatBgColor)), // Set the height as needed
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_icon))
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever,modifier = Modifier.size(58.dp) )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "GemBot", fontSize = 22.sp, color = Color.Black)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { onClearChat() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Black)
                    }
                },
                colors = topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    }
}