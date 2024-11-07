@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bands.utils

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.data.api.WeatherModel
import com.example.bands.di.BandsViewModel
import com.example.bands.ui.theme.Typography
import java.util.UUID

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
fun CommonTitleText(text: String,
                    data: WeatherModel? = null,
                    showSearchBar: Boolean = false,
                    searchQuery: String = "",
                    onSearchQueryChange: (String) -> Unit = {}) {
    val expanded = remember { mutableStateOf(false) }
    val ifSearchExpanded = remember { mutableStateOf(false) }

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
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                )
                if (showSearchBar) {
                    if (ifSearchExpanded.value) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .padding(1.dp)
                                .clip(CircleShape)
                                .height(46.dp)
                                .border(1.dp, Color.Gray, CircleShape),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Gray,
                            ),
                            textStyle = TextStyle(fontSize = 14.sp),
                            trailingIcon = {
                                IconButton(onClick = { ifSearchExpanded.value = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        IconButton(onClick = { ifSearchExpanded.value = true }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Open Search")
                        }
                    }
                }
            }
            if (data!= null) {
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
fun CommonRow(
    imageUrl: String?,
    name: String?,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val key = remember { UUID.randomUUID() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
//            .clickable { onItemClick.invoke() },
            .pointerInput(key) {
                detectTapGestures(
                    onTap = { onItemClick() },
                    onLongPress = { onLongClick() }
                )
            },
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
            if (imageUrl == "") {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_icon))
                LottieAnimation(
                    composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(58.dp)
                )
            } else {
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
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )
        Text(
            text = name ?: "---", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier
                .padding(top =2.dp)
        )
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

@Composable
fun WeatherShowText(
    data: WeatherModel
) {
    val isScrollingEnabled = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(50), false) // Deeper shadow
                .shadow(8.dp, RoundedCornerShape(50))
                .background(
                    //brush = Brush.horizontalGradient( colors = listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA))
                    //brush=Brush.horizontalGradient(colors = listOf(Color(0xFFA7C6D9), Color(0xFFB9E7D9))
                    //brush = Brush.horizontalGradient( colors = listOf(Color(0xFFFFB3A7), Color(0xFFFF7F50))
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFC4B69D), Color(0xFFE0CDA9))
                    ),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable {
                    isScrollingEnabled.value = true
                    focusRequester.requestFocus()
                }
                .focusRequester(focusRequester)
                .focusable(enabled = true).align(Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 4.dp),
                    model = "https:${data.current.condition.icon}".replace("64x64", "128x128"),
                    contentDescription = "icon",
                )
                Text(
                    text = "${data.current.temp_c}°C",
                    color = Color(0xFF4E3B31),
                    fontWeight = FontWeight.Medium
                )

            }
        }
        if (isScrollingEnabled.value) {
            ScrollingWeather(data, focusRequester, isScrollingEnabled)
        }
    }
}

@Composable
fun ScrollingWeather(
    data: WeatherModel,
    focusRequester: FocusRequester,
    isScrollingEnabled: MutableState<Boolean>
) {
    Row(
        modifier = Modifier
            .basicMarquee(
                animationMode = MarqueeAnimationMode.Immediately,
                velocity = if (isScrollingEnabled.value) 100.dp else 0.dp
            )
            .focusRequester(focusRequester)
            .focusable()
    ) {
        GradientCapsule(text1 = data.location.name, text2 = "")
        GradientCapsule(text1 = data.current.condition.text, text2 = "Clouds")
        GradientCapsule(text1 = data.location.localtime.split(" ")[0], text2 = "")
        GradientCapsule(text1 = data.current.humidity, text2 = "Humidity")
        GradientCapsule(text1 = data.current.wind_kph + "km/h", text2 = "Wind Speed")
        GradientCapsule(text1 = data.current.uv, text2 = "UV")
        GradientCapsule(text1 = "${data.current.feelslike_c}°C", text2 = "Feels like")
    }
}

@Composable
fun GradientCapsule(
    text1: String,
    text2: String,
) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(50), false) // Deeper shadow
                .shadow(4.dp, RoundedCornerShape(50))
                .background(
                    //brush = Brush.horizontalGradient( colors = listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA))
                    //brush=Brush.horizontalGradient(colors = listOf(Color(0xFFA7C6D9), Color(0xFFB9E7D9))
                    //brush = Brush.horizontalGradient( colors = listOf(Color(0xFFFFB3A7), Color(0xFFFF7F50))
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFC4B69D), Color(0xFFE0CDA9))
                    ),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row {
                Text(
                    text = "$text1 ",
                    color = Color(0xFF4E3B31),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = text2,
                    color = Color(0xFF4E3B31),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}
class RingtonePlayer(private val context:Context){
    private var ringtone:Ringtone?=null
    fun playRingTone(){
        val ringtoneUri:Uri =RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone?.play()
    }
    fun stopRingtone(){
        ringtone?.stop()
    }
}
