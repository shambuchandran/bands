package com.example.bands.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.di.BandsViewModel
import com.example.bands.utils.CommonDivider
import com.example.bands.utils.CommonImage
import com.example.bands.utils.CommonProgressBar
import com.example.bands.utils.navigateTo

@Composable
fun ProfileScreen(navController: NavController, viewModel: BandsViewModel) {
    val inProgress = viewModel.inProgress.value
    if (inProgress) {
        CommonProgressBar()
    } else {

        val userData =viewModel.userData.value
        var name by rememberSaveable {
            mutableStateOf(userData?.name?:"")
        }
        var phoneNumber by rememberSaveable {
            mutableStateOf(userData?.phoneNumber?:"")
        }
        Column {
            ProfileContent(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                viewModel = viewModel,
                name = name,
                phoneNumber = phoneNumber,
                onNameChange = { name = it },
                onPhoneNumberChange = { phoneNumber = it },
                onSave = { viewModel.createOrUpdateProfile(name = name,phoneNumber = phoneNumber)
                },
                onLogout = {viewModel.logout()
                    // navigateTo(navController,DestinationScreen.Login.route)
                    navigateTo(navController,DestinationScreen.PhoneAuth.route)
                },
                onBack = { navigateTo(navController,DestinationScreen.ChatList.route) }
            )
            BottomNavigationMenu(
                selectedItem = BottomNavigationItem.PROFILE,
                navController = navController
            )
        }
    }
}

@Composable
fun ProfileContent(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BandsViewModel,
    name: String,
    phoneNumber: String,
    onNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = viewModel.userData.value?.imageUrl

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        color = colorResource(id = R.color.BgColor),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Back", modifier = Modifier.clickable { onBack.invoke() })
                Text(text = "Save", modifier = Modifier.clickable { onSave.invoke() })
            }
            CommonDivider()
            ProfileImage(viewModel = viewModel, imageUrl = imageUrl)
            CommonDivider()
            Row(
                Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.Center
            ) {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(text = "Name") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp), horizontalArrangement = Arrangement.Center
            ) {
                TextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = { Text(text = "Phone Number") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        disabledTextColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        disabledIndicatorColor = Color.Black,
                    ), enabled = false
                )
            }
            CommonDivider()
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Logout", Modifier.clickable { onLogout.invoke() })
            }
        }
    }
}

@Composable
fun ProfileImage(imageUrl: String?, viewModel: BandsViewModel) {
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.uploadProfileImage(uri)
            }
        }
    Box(modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable {
                    launcher.launch("image/*")
                }, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = CircleShape, modifier = Modifier
                    .padding(2.dp)
                    .size(108.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(
                    width = 2.dp,
                    color = Color.Black,
                    shape = CircleShape
                )
            ) {
                ImageAtProfile(data = imageUrl)
            }
            Text(text = "Change Profile Pic")
        }
        if (viewModel.inProgress.value) CommonProgressBar()
    }

}
@Composable
fun ImageAtProfile(
    data: String?,
    modifier: Modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.BgColor)),
    contentScale: ContentScale = ContentScale.Fit,
) {
    val painter = rememberAsyncImagePainter(model = data)
    Image(
        painter = painter,
        contentDescription = "Image",
        modifier = modifier.padding(12.dp),
        contentScale = contentScale
    )

}
