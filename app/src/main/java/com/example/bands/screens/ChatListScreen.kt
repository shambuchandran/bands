package com.example.bands.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.ChatData
import com.example.bands.data.ChatUser
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallViewModel
import com.example.bands.utils.CommonProgressBar
import com.example.bands.utils.CommonRow
import com.example.bands.utils.CommonTitleText
import com.example.bands.utils.navigateTo
import org.webrtc.SurfaceViewRenderer
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: BandsViewModel,
    callViewModel: CallViewModel
) {

    val inProgress = viewModel.inProgressChats
    if (inProgress.value) {
        CommonProgressBar()
    } else {
        val chats = viewModel.chats.value.toMutableList()
        val userData = viewModel.userData.value
        val showDialog = remember {
            mutableStateOf(false)
        }
        val showChildFab = remember { mutableStateOf(false) }
        val showStickyHeader = viewModel.showStickyHeader
        val onFabClick: () -> Unit = {
            if (showChildFab.value) {
                showDialog.value = true
            }
            showChildFab.value = !showChildFab.value
        }
        val onDismiss: () -> Unit = {
            showDialog.value = false
            showChildFab.value = false
        }
        val onAddChat: (String) -> Unit = {
            viewModel.onAddChat(it)
            showDialog.value = false
            showChildFab.value = false
        }
        val image = drawableToBase64(LocalContext.current, R.drawable.cyborg_11260851)

        val onAddAiChat: () -> Unit = {

//            val gemBotChat = ChatData(
//                chatId = "gem_bot",
//                user1 = ChatUser(
//                    userId = userData?.userId ?: "unknown",
//                    name = userData?.name ?: "You"
//                ),
//                user2 = ChatUser(
//                    userId = "Gem-Bot",
//                    name = "GemBot",
//                    imageUrl = ""
//                )
//            )
//            chats.add(0, gemBotChat)
            viewModel.toggleStickyHeader()
            showDialog.value = false
            showChildFab.value = false

        }
        val context = LocalContext.current
        val localViewRenderer = remember { SurfaceViewRenderer(context) }
        val remoteViewRenderer = remember { SurfaceViewRenderer(context) }

        LaunchedEffect(key1 = Unit) {
            userData?.phoneNumber?.let {
                callViewModel.init(it)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                localViewRenderer.release()
                remoteViewRenderer.release()
            }
        }
        Scaffold(
            floatingActionButton = {
                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    AnimatedVisibility(
                        visible = showChildFab.value,
                        enter = fadeIn(animationSpec = tween(durationMillis = 700)) + slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 700)
                        ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 700)) + slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 700)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-150).dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                onAddAiChat()
                                showChildFab.value = false
                            },
                            containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Gem-Bot",
                                tint = Color.White
                            )
                        }
                    }
                    Fab(
                        showDialog = showDialog.value,
                        onFabClick = onFabClick,
                        onDismiss = onDismiss,
                        onAddChat = onAddChat
                    )
                }
            }, content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    CommonTitleText(text = "Chats")
                    if (chats.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "No chats Available")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 4.dp)
                        ) {
                            if (showStickyHeader) {
                                stickyHeader {
                                    CommonRow(imageUrl =image , name = "Gem Bot", onItemClick = {
                                        Toast.makeText(context,"ai page",Toast.LENGTH_SHORT).show()
                                    })
                                }
                            }
                            itemsIndexed(chats) { index, chat ->
                                val chatUser =
                                    if (chat.user1.userId == userData?.userId) chat.user2 else chat.user1
                                CommonRow(
                                    imageUrl = chatUser.imageUrl,
                                    name = chatUser.name
                                ) {
                                    chat.chatId?.let {
                                        navigateTo(
                                            navController,
                                            DestinationScreen.SingleChat.createRoute(id = it)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    BottomNavigationMenu(
                        selectedItem = BottomNavigationItem.CHATLIST,
                        navController = navController
                    )
                }
            })
    }

}

@Composable
fun Fab(
    showDialog: Boolean,
    onFabClick: () -> Unit,
    onDismiss: () -> Unit,
    onAddChat: (String) -> Unit
) {
    val addChatNumber = remember {
        mutableStateOf("")
    }
    if (showDialog) {
        AlertDialog(onDismissRequest = {
            onDismiss.invoke()
            addChatNumber.value = ""
        }, confirmButton = {
            Button(onClick = { onAddChat(addChatNumber.value) }) {
                Text(text = "Add Chat")
            }
        },
            title = { Text(text = "Add Chat") },
            text = {
                OutlinedTextField(
                    value = addChatNumber.value,
                    label = { Text(text = "Phone Number") },
                    onValueChange = { addChatNumber.value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            })

    }
    FloatingActionButton(
        onClick = { onFabClick() },
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 78.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add chat",
            tint = Color.White
        )

    }


}

fun drawableToBase64(context: Context, drawableId: Int): String {
    val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}
