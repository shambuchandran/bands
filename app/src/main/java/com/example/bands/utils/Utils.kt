package com.example.bands.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.MessageModel
import com.example.bands.di.BandsViewModel
import com.example.bands.ui.theme.Typography

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
            .alpha(.5f)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun CommonImage(
    data: String?,
    modifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val painter = rememberAsyncImagePainter(model = data)
    Image(
        painter = painter,
        contentDescription = "Image",
        modifier = modifier.clip(CircleShape),
        contentScale = contentScale
    )

}

@Composable
fun CommonTitleText(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd =  20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun CommonRow(
    imageUrl: String?,
    name: String?,
    onItemClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clickable { onItemClick.invoke() },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = Color(0xFF3C5A7A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            if (imageUrl==""){
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_icon))
                LottieAnimation(composition, iterations = LottieConstants.IterateForever,modifier = Modifier.size(58.dp) )
            }else{
                CommonImage(
                    data = imageUrl,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Text(
                text = name ?: "---",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
@Composable
fun CommonStatus(imageUrl: String?, name: String?, onItemClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(2.dp)
            .width(72.dp)
            .height(100.dp)
            .clickable { onItemClick.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CommonImage(
            data = imageUrl,
            modifier = Modifier
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

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val BgColor = Color(0xFFEDE1D1)
val AuthTextColor = Color(0xFF515532)
val ChatBgColor = Color(0xFF304F6D)


@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        // Dark theme colors
        darkColorScheme(
            primary = Black,
            onPrimary = White,
            background = ChatBgColor,
            surface = ChatBgColor,
            onSurface = AuthTextColor
        )
    } else {
        // Light theme colors
        lightColorScheme(
            primary = Black,
            onPrimary = White,
            background = ChatBgColor,
            surface = ChatBgColor,
            onSurface = AuthTextColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}