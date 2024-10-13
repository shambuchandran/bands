package com.example.bands

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import com.example.bands.data.MessageModel
import com.example.bands.di.BandsViewModel
import com.example.bands.di.CallViewModel
import com.example.bands.screens.ChatListScreen
import com.example.bands.screens.LoginScreen
import com.example.bands.screens.PhoneAuthScreen
import com.example.bands.screens.ProfileScreen
import com.example.bands.screens.SignUpScreen
import com.example.bands.screens.SingleChatScreen
import com.example.bands.screens.SingleStatusScreen
import com.example.bands.screens.StatusScreen
import com.example.bands.ui.theme.BandsTheme
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint

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

}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            BandsTheme {
                // A surface container using the 'background' color from the theme
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


    @Composable
    fun BandsAppNavigation() {

        val navController = rememberNavController()
        val viewModel = hiltViewModel<BandsViewModel>()
        val callViewModel = hiltViewModel<CallViewModel>()
        val incomingCallState = callViewModel.incomingCallerSession.collectAsState(null)

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
                    ChatListScreen(navController, viewModel)
                }
                composable(DestinationScreen.SingleChat.route) {
                    val chatId = it.arguments?.getString("chatId")
                    chatId?.let { SingleChatScreen(navController, viewModel, chatId = chatId, callViewModel =callViewModel ) }

                }
                composable(DestinationScreen.Profile.route) {
                    ProfileScreen(navController, viewModel)
                }
                composable(DestinationScreen.StatusList.route) {
                    StatusScreen(navController, viewModel)
                }
                composable(DestinationScreen.SingleStatus.route) {
                    val userId = it.arguments?.getString("userId")
                    userId?.let {
                        SingleStatusScreen(navController, viewModel, userId = it)
                    }
                }
            }
                Log.d("IncomingCallComponent", incomingCallState.toString())
            if (incomingCallState.value != null) {
                IncomingCallComponent(
                    incomingCallerName = incomingCallState.value?.name,
                    onAcceptPressed = callViewModel::acceptCall,
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
            Manifest.permission.SYSTEM_ALERT_WINDOW)
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
       onAcceptPressed: () -> Unit,
       onRejectPressed: () -> Unit
   ) {
        if(incomingCallerName != null){
            Row(modifier= Modifier
                .fillMaxWidth()
                .height(82.dp)
                .zIndex(1f)
                .border(0.5.dp, color = Color.LightGray)
                .background(color = Color.LightGray, shape = RoundedCornerShape(50.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "$incomingCallerName is calling",
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
                        .clickable { onAcceptPressed.invoke() }
                )

                Image(
                    painter = painterResource(id = R.drawable.baseline_call_end_24),
                    contentDescription = "Reject Call",
                    modifier = Modifier
                        .weight(2f)
                        .padding(end = 8.dp)
                        .clickable { onRejectPressed.invoke() }
                )
            }
        }
    }
}



