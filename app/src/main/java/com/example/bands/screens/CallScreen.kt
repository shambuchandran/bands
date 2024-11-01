package com.example.bands.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CallScreen(name:String?,phoneNumber: String?, callType: String?) {
    if (callType == "video") {
        Text("Starting Video Call with $name $phoneNumber")
        // Add video call UI and functionality
    } else if (callType == "audio") {
        Text("Starting Audio Call with $name $phoneNumber")
        // Add audio call UI and functionality
    }

}