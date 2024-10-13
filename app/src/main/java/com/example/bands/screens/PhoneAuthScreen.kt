package com.example.bands.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bands.di.BandsViewModel
import com.example.bands.utils.CheckIsSignedIn
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.utils.navigateTo
import com.togitech.ccp.component.TogiCountryCodePicker

@Composable
fun PhoneAuthScreen(navController: NavHostController, viewModel: BandsViewModel) {
    CheckIsSignedIn(viewModel = viewModel, navController = navController)

    var phoneNumber by rememberSaveable {
        mutableStateOf("")
    }
    var fullPhoneNumber by rememberSaveable {
        mutableStateOf("")
    }
    var isPhoneNumberValid by rememberSaveable {
        mutableStateOf(false)
    }
    var showOtpField by rememberSaveable {  // Dynamic based on phone number validation
        mutableStateOf(false)
    }
    var otpReceived by remember {
        mutableStateOf("")
    }
    var currentActivity= LocalContext.current as Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.BgColor))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .width(220.dp)
                    .padding(16.dp)
            )
            Text(
                text = "Register",
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
                color = colorResource(id = R.color.AuthTextColor)
            )
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country code picker
                TogiCountryCodePicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    initialCountryIsoCode = "+91",
                    initialCountryPhoneCode = "+91",
                    onValueChange = { (code, phone), isValid ->
                        Log.d("CCP", "onValueChange: $code $phone -> $isValid")
                        phoneNumber = phone
                        fullPhoneNumber = code + phone
                        isPhoneNumberValid = isValid
                        showOtpField = isPhoneNumberValid  // Show OTP field only when the phone number is valid
                    },
                    label = { Text(text = "Enter Phone Number") }
                )
            }

            // Request OTP button (visible only if the phone number is valid)
            if (isPhoneNumberValid) {
                Button(onClick = {
                    // Logic to request OTP
                    viewModel.startPhoneNumberVerification(fullPhoneNumber,currentActivity)
                    showOtpField = true // Set true after OTP request
                    Log.d("OTP", "Requesting OTP for: $fullPhoneNumber")
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Request OTP")
                }
            }

            // Show OTP field after requesting OTP
            if (showOtpField) {
                OtpTextField(
                    otpReceived = otpReceived,
                    onOtpChange = { otpReceived = it }
                )

                Button(onClick = {
                    // Handle OTP verification
                    phoneNumber=""
                    viewModel.verifyOtp(otpReceived)
                    Log.d("OTP", "Verifying OTP: $otpReceived")
                    navigateTo(navController,DestinationScreen.Profile.route)
                    isPhoneNumberValid=false
                    showOtpField=false
                }, modifier = Modifier.padding(8.dp)) {
                    Text(text = "Verify the OTP")
                }

                TextButton(onClick = {
                    // Handle resend OTP logic
                    viewModel.resendVerificationCode(fullPhoneNumber,currentActivity)
                    Log.d("OTP", "Resending OTP for: $fullPhoneNumber")
                }) {
                    Text(
                        text = "Resend OTP",
                        modifier = Modifier.padding(8.dp),
                        color = colorResource(id = R.color.AuthTextColor)
                    )
                }
            }
        }
    }
}

@Composable
fun OtpTextField(otpReceived: String, onOtpChange: (String) -> Unit) {
    Text(
        text = "Enter the OTP received",
        modifier = Modifier.padding(8.dp),
        color = colorResource(id = R.color.AuthTextColor)
    )
    BasicTextField(value = otpReceived, onValueChange = {
        if (it.length <= 6) {
            onOtpChange(it)
        }
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { index ->
                val number = when {
                    index >= otpReceived.length -> ""
                    else -> otpReceived[index].toString()
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = number, style = MaterialTheme.typography.titleLarge)
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}