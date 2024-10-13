package com.example.bands.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bands.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseMessageService : FirebaseMessagingService(){
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message received: ${message.data}")

        message.data["senderId"]?.let { senderId ->
            Firebase.auth.currentUser?.let { currentUser ->
                if (senderId == currentUser.uid) {
                    Log.d("FCM", "Skipping notification for the sender")
                    return
                }
            }
        }
        message.notification?.let {
            Log.d("FCM", "Notification Title: ${it.title}, Body: ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(title:String?,message:String?) {
        val notificationManager =getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(this,"messages")
            .setContentTitle(title ?: "No Title")
            .setContentText(message ?: "No Message")
            .setSmallIcon(R.drawable.messages)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId,notification)

    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for chat messages"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


}