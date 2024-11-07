package com.example.bands.screens

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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bands.data.CallLog
import com.example.bands.di.CallStatus
import com.example.bands.di.CallViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallLogsScreen(viewModel: CallViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchCallLogs()
    }
    val callLogs by viewModel.callLogs.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        items(callLogs) { callLog ->
            CallLogItem(callLog) { isAudioCall ->
                viewModel.startCall(callLog.target, if (isAudioCall) "true" else "false")
            }
        }
    }
}

@Composable
fun CallLogItem(callLog: CallLog, onCallClick: (Boolean) -> Unit) {
    val formattedEndTime = callLog.endTime?.let { formatTime(it) }
    //val callDuration = callLog.endTime?.let { formatDuration(callLog.startTime, it) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCallClick(callLog.callType == "audio") }
            //.padding(8.dp),
        ,elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    //text = "${callLog.caller} -> ${callLog.target}",
                    text = callLog.target,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                formattedEndTime?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
//                callDuration?.let {
//                    Text(
//                        text = "Duration: $it",
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                    )
//                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = callLog.status,
                fontSize = 12.sp,
                color = when (callLog.status) {
                    CallStatus.MISSED.name -> Color.Red
                    CallStatus.REJECTED.name -> Color.Gray
                    "completed" -> Color.Green
                    else -> Color.White
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (callLog.callType == "audio") Icons.Default.Phone else Icons.Default.VideoCall,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatTime(timeInMillis: Long): String {
    val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timeInMillis))
}

//fun formatDuration(startTime: Long, endTime: Long): String {
//    val duration = endTime - startTime
//    val seconds = (duration / 1000) % 60
//    val minutes = (duration / (1000 * 60)) % 60
//    val hours = (duration / (1000 * 60 * 60)) % 24
//
//    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
//}
