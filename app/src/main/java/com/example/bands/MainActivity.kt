package com.example.bands

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallViewModel
import com.example.bands.di.GemBotViewModel
import com.example.bands.di.WeatherViewModel
import com.example.bands.screens.CallScreen
import com.example.bands.screens.ChatListScreen
import com.example.bands.screens.GemChatPage
import com.example.bands.screens.LoginScreen
import com.example.bands.screens.NewsArticleScreen
import com.example.bands.screens.PhoneAuthScreen
import com.example.bands.screens.ProfileScreen
import com.example.bands.screens.SignUpScreen
import com.example.bands.screens.SingleChatScreen
import com.example.bands.screens.SingleStatusScreen
import com.example.bands.screens.StatusScreen
import com.example.bands.utils.MyAppTheme
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

sealed class DestinationScreen(var route: String) {
    object SignUp : DestinationScreen("signup")
    object PhoneAuth : DestinationScreen("phoneAuth")
    object Login : DestinationScreen("login")
    object Profile : DestinationScreen("profile")
    object ChatList : DestinationScreen("chatList")
    object SingleChat : DestinationScreen("singleChat/{chatId}") {
        fun createRoute(id: String) = "singleChat/$id"
    }
    object StatusList : DestinationScreen("statusList")
    object SingleStatus : DestinationScreen("singleStatus/{userId}") {
        fun createRoute(userId: String) = "singleStatus/$userId"
    }
    object GemChatPage : DestinationScreen("gemChatPage")
    @Serializable
    data class NewsArticleScreenRoute(val url: String)
    object CallScreen : DestinationScreen("call/{name}/{phoneNumber}/{isAudioCall}") {
        fun createRoute( name:String,phoneNumber: String,isAudioCall:Boolean) = "call/$name/$phoneNumber/$isAudioCall"
    }

}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            // BandsTheme {
            MyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BandsAppNavigation()
                }
            }
        }
        permissions { }
    }

    @SuppressLint("SuspiciousIndentation")
    @Composable
    fun BandsAppNavigation() {
        val navController = rememberNavController()
        val viewModel = hiltViewModel<BandsViewModel>()
        val callViewModel = hiltViewModel<CallViewModel>()
        val gemBotViewModel = hiltViewModel<GemBotViewModel>()
        val weatherViewModel = hiltViewModel<WeatherViewModel>()
        val incomingCallState = callViewModel.incomingCallerSession.collectAsState(null)
        val isInCall =callViewModel.isInCall.collectAsState(false)
        Box {
            NavHost(
                navController = navController,
                startDestination = DestinationScreen.PhoneAuth.route
            ) {
                composable(DestinationScreen.PhoneAuth.route) {
                    PhoneAuthScreen(navController, viewModel)
                }
                composable(DestinationScreen.SignUp.route) {
                    SignUpScreen(navController, viewModel)
                }
                composable(DestinationScreen.Login.route) {
                    LoginScreen(navController, viewModel)
                }
                composable(DestinationScreen.ChatList.route) {
                    ChatListScreen(navController, viewModel, callViewModel, weatherViewModel)
                }
                composable(DestinationScreen.SingleChat.route) {
                    val chatId = it.arguments?.getString("chatId")
                    chatId?.let {
                        SingleChatScreen(
                            navController,
                            viewModel,
                            chatId = it,
                            callViewModel = callViewModel,
                            weatherViewModel = weatherViewModel
                        )
                    }

                }
                composable(DestinationScreen.GemChatPage.route) {
                    GemChatPage(
                        viewModel = gemBotViewModel,
                        navController = navController,
                        context = applicationContext
                    )
                }
                composable(DestinationScreen.Profile.route) {
                    ProfileScreen(navController, viewModel)
                }
                composable(DestinationScreen.StatusList.route) {
                    StatusScreen(navController, viewModel, weatherViewModel)
                }
                composable(DestinationScreen.SingleStatus.route) {
                    val userId = it.arguments?.getString("userId")
                    userId?.let {
                        SingleStatusScreen(navController, viewModel, userId = it)
                    }
                }
                composable<DestinationScreen.NewsArticleScreenRoute> {
                    val argument = it.toRoute<DestinationScreen.NewsArticleScreenRoute>()
                    NewsArticleScreen(argument.url)
                }
                composable(DestinationScreen.CallScreen.route) { backStackEntry ->
                    val name=backStackEntry.arguments?.getString("name")
                    val phoneNumber = backStackEntry.arguments?.getString("phoneNumber")
                    val isAudioCall =backStackEntry.arguments?.getBoolean("isAudioCall")
                    Log.d("RTCC call click ","$isAudioCall")
                    if (phoneNumber != null) {
                            if (name != null) {
                                if (isAudioCall != null) {
                                    CallScreen(name,phoneNumber,isAudioCall,callViewModel,navController)
                                }
                            }
                    }
                }

            }
            Log.d("RTCC icc", " icc ${incomingCallState.value.toString()}")
            if (incomingCallState.value != null && !isInCall.value) {
                IncomingCallComponent(
                    incomingCallerName = incomingCallState.value?.name,
                    incomingCallerNumber = incomingCallState.value?.name,
                    onAcceptPressed = {
                        navController.navigate(
                            DestinationScreen.CallScreen.createRoute(incomingCallState.value?.name ?:"",incomingCallState.value?.name ?: "",incomingCallState.value?.isAudioOnly?: false )
                        )
                        //callViewModel.acceptCall()
                        callViewModel.setCallAcceptedPending(true)
                    },
                    onRejectPressed = callViewModel::rejectCall
                )
            }

        }
    }

    private fun permissions(
        onGranted: () -> Unit,
    ) {
        PermissionX.init(this).permissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
            .onExplainRequestReason { scope, deniedList ->
                val message =
                    "We need your consent for the following " +
                            "permissions in order to use the functions properly"
                scope.showRequestReasonDialog(
                    deniedList, message, "Allow", "Deny"
                )
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    onGranted()
                }
            }
    }

    @Composable
    fun IncomingCallComponent(
        incomingCallerName: String?,
        incomingCallerNumber: String?,
        onAcceptPressed: () -> Unit,
        onRejectPressed: () -> Unit
    ) {
        if (incomingCallerName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .zIndex(1f)
                    .border(0.5.dp, color = Color.LightGray)
                    .background(color = Color.LightGray, shape = RoundedCornerShape(50.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (incomingCallerName == null)"$incomingCallerNumber" else "$incomingCallerName +is calling",
                    modifier = Modifier
                        .weight(8f)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = Color.Black
                )

                Image(
                    painter = painterResource(id = R.drawable.baseline_call_24),
                    contentDescription = "Accept Call",
                    modifier = Modifier
                        .weight(2f)
                        .padding(end = 8.dp)
                        .clickable {
                            if (incomingCallerNumber != null) {
                                onAcceptPressed.invoke()
                            }
                        }
                )

                Image(
                    painter = painterResource(id = R.drawable.baseline_call_end_24),
                    contentDescription = "Reject Call",
                    modifier = Modifier
                        .weight(2f)
                        .padding(end = 8.dp)
                        .clickable {
                            onRejectPressed.invoke()
                        }
                )
            }
        }
    }
}



