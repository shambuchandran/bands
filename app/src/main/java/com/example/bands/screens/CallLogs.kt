package com.example.bands.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bands.data.CallLog
import com.example.bands.di.CallStatus
import com.example.bands.di.CallViewModel
import com.example.bands.utils.CommonTitleText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallLogsScreen(viewModel: CallViewModel,navController:NavController) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.fetchCallLogs()
    }
    val callLogs by viewModel.callLogs.collectAsState()
    Column {
        CommonTitleText(text = "Calls", showSearchBar = false, showMenuIcon = true, onDeleteConfirm ={
            viewModel.deleteAllCallLogs()
        } )
        LazyColumn(
            modifier = Modifier.padding(horizontal = 4.dp).weight(1f)
        ) {
            items(callLogs.sortedByDescending { it.endTime?:it.startTime}) { callLog ->
                CallLogItem(callLog,viewModel) { isAudioCall ->
                    //if (isAudioCall == "true") "true" else "false"
                    Toast.makeText(context,"This feature is upcoming",Toast.LENGTH_SHORT).show()
                }
            }
        }
        BottomNavigationMenu(
            selectedItem = BottomNavigationItem.CALLLOGS,
            navController = navController
        )
    }
}

@Composable
fun CallLogItem(callLog: CallLog,viewModel: CallViewModel, onCallClick: (String) -> Unit) {
    val formattedEndTime = formatTime(callLog.endTime ?: callLog.startTime)
    val isIncomingCall = callLog.caller != viewModel.userName
    val arrowIcon = if (isIncomingCall) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val arrowColor = when (callLog.status.uppercase(Locale.ROOT)) {
        CallStatus.MISSED.name -> Color.Red
        CallStatus.REJECTED.name -> Color.Yellow
        "COMPLETED" -> Color.Green
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical =4.dp)
        ,elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
        color = Color.Gray
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = arrowIcon,
                contentDescription = null,
                tint = arrowColor,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp)
                    .graphicsLayer(rotationZ = -35f)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    //text = "${callLog.caller} -> ${callLog.target}",
                    text = if (isIncomingCall) callLog.caller else callLog.target,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                formattedEndTime?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = callLog.status.uppercase(Locale.ROOT),
                fontSize = 12.sp,
                color = when (callLog.status.uppercase(Locale.ROOT)) {
                    CallStatus.MISSED.name -> Color.Red
                    CallStatus.REJECTED.name -> Color.Yellow
                    "COMPLETED" -> Color.Green
                    else -> Color.White
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (callLog.callType == "audio") Icons.Default.Phone else Icons.Default.VideoCall,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onCallClick(if (callLog.callType == "audio") "true" else "false") }
            )
        }
    }
}
fun formatTime(timeInMillis: Long): String {
    val dateFormat = SimpleDateFormat("MM/dd/yy hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(timeInMillis))
}

