package com.example.bands.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bands.data.GemMessageModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class GemBotViewModel @Inject constructor(
    private val generativeModel: GenerativeModel
) : ViewModel() {
    //val messageList by lazy { mutableStateListOf<GemMessageModel>() }
    val messageList = mutableStateListOf<GemMessageModel>()
    fun sendMessage(question: String, applicationContext: Context) {
        if (!isInternetAvailable(applicationContext)) {
            messageList.add(
                GemMessageModel(
                    "Error: No internet connection",
                    "model",
                    System.currentTimeMillis()
                )
            )
            return
        }
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                messageList.add(GemMessageModel(question, "user", currentTime))
                messageList.add(GemMessageModel("Typing...", "model", currentTime))

                val chat = generativeModel.startChat(
                    history = messageList.map {
                        content(it.role) { text(it.message) }
                    }.toList()
                )
                val response = chat.sendMessage(question)
                messageList.removeLast()
                messageList.add(
                    GemMessageModel(
                        response.text.toString(),
                        "model",
                        System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                messageList.removeLast()
                messageList.add(
                    GemMessageModel(
                        "Error: ${e.message}",
                        "model",
                        System.currentTimeMillis()
                    )
                )
            }
        }
    }
    fun clearChat() {
        messageList.clear()
    }
    private fun isInternetAvailable(applicationContext: Context): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)
        }
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

}