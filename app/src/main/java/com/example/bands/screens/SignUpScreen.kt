package com.example.bands.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bands.di.BandsViewModel
import com.example.bands.utils.CheckIsSignedIn
import com.example.bands.utils.CommonProgressBar
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.utils.navigateTo

@Composable
fun SignUpScreen(navController: NavController, viewModel: BandsViewModel) {
    CheckIsSignedIn(viewModel = viewModel, navController = navController )
    Box(modifier = Modifier
        .fillMaxSize()
        .background(colorResource(id = R.color.BgColor))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
                .verticalScroll(
                    rememberScrollState()
                ), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .width(220.dp)
                    .padding(16.dp)
            )
            Text(
                text = "SignUp",
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), color = colorResource(id = R.color.AuthTextColor)
            )
            val nameState = remember {
                mutableStateOf(TextFieldValue())
            }
            val numberState = remember {
                mutableStateOf(TextFieldValue())
            }
            val emailState = remember {
                mutableStateOf(TextFieldValue())
            }
            val passwordState = remember {
                mutableStateOf(TextFieldValue())
            }
            //val focus = LocalFocusManager.current
            OutlinedTextField(value = nameState.value, onValueChange = { nameState.value = it }, label = { Text(
                text = "Name"
            )}, modifier = Modifier.padding(8.dp))
            OutlinedTextField(value = numberState.value, onValueChange = { numberState.value = it }, label = { Text(
                text = "Phone Number"
            )}, modifier = Modifier.padding(8.dp))
            OutlinedTextField(value = emailState.value, onValueChange = { emailState.value = it }, label = { Text(
                text = "Email"
            )}, modifier = Modifier.padding(8.dp))
            OutlinedTextField(value = passwordState.value, onValueChange = { passwordState.value = it }, label = { Text(
                text = "Password"
            )}, modifier = Modifier.padding(8.dp))
            Button(onClick = { viewModel.signUp(nameState.value.text,numberState.value.text,emailState.value.text,passwordState.value.text) },
                Modifier
                    .padding(8.dp)) {
                Text(text = "SignUp")
            }
            TextButton(onClick = { navigateTo(navController,DestinationScreen.Login.route) }) {
                Text(text = "Already a user? Login", modifier = Modifier.padding(8.dp), color = colorResource(id = R.color.AuthTextColor))
            }
        }
        if (viewModel.inProgress.value){
            CommonProgressBar()
        }


    }

}