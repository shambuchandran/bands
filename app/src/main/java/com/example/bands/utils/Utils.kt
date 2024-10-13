package com.example.bands.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.bands.di.BandsViewModel
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.MessageModel

fun navigateTo(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(route)
        launchSingleTop = true
    }
}

@Composable
fun CommonDivider() {
    Divider(
        color = Color.LightGray, thickness = 1.dp, modifier = Modifier
            .alpha(.3f)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun CommonImage(
    data: String?,
    modifier: Modifier = Modifier.wrapContentSize(),
    contentScale: ContentScale = ContentScale.Crop
) {
    val painter = rememberImagePainter(data = data)
    Image(
        painter = painter,
        contentDescription = "Image",
        modifier = modifier,
        contentScale = contentScale
    )

}

@Composable
fun CommonTitleText(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun CommonRow(imageUrl: String?, name: String?, onItemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onItemClick.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommonImage(data = imageUrl, modifier = Modifier
            .padding(8.dp)
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.Gray))
        Text(text = name?:"---", fontWeight = FontWeight.Bold,modifier = Modifier
            .padding(start = 4.dp))
    }


}

@Composable
fun CommonProgressBar() {
    Row(
        modifier = Modifier
            .alpha(.5f)
            .background(color = colorResource(id = R.color.AuthTextColor))
            .fillMaxSize()
            .clickable(enabled = false) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun CheckIsSignedIn(viewModel: BandsViewModel, navController: NavController) {
    val alreadySignIn = remember {
        mutableStateOf(false)
    }
    val signIn = viewModel.signIn.value
    if (signIn && !alreadySignIn.value) {
        alreadySignIn.value = true
        navController.navigate(DestinationScreen.ChatList.route) {
            popUpTo(0)
        }
    }
}

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}