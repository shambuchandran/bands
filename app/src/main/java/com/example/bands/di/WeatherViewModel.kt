package com.example.bands.di

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bands.data.CHATS
import com.example.bands.data.ChatData
import com.example.bands.data.api.Constant
import com.example.bands.data.api.WeatherModel
import com.example.bands.weatherupdates.GeolocationApi
import com.example.bands.weatherupdates.NetworkResponse
import com.example.bands.weatherupdates.WeatherApiInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherApiInterface: WeatherApiInterface,
    private val geolocationApi: GeolocationApi,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _weatherResult = MutableLiveData<NetworkResponse<WeatherModel>>()
    val weatherResult: LiveData<NetworkResponse<WeatherModel>> = _weatherResult
    private val _chatUserCityName = MutableLiveData<NetworkResponse<WeatherModel>>()
    val chatUserCityName: LiveData<NetworkResponse<WeatherModel>> = _chatUserCityName

    init {
        getCityFromIpAndFetchWeather()
    }

    private fun getCityFromIpAndFetchWeather() {
        viewModelScope.launch {
            try {
                val geoResponse = geolocationApi.getLocationData()
                if (geoResponse.isSuccessful) {
                    geoResponse.body()?.let { location ->
                        val cityName = location.city
                        getData(cityName)
                        updateUserCity(cityName)
                    } ?: run {
                        _weatherResult.value =
                            NetworkResponse.Error("Failed to load: Empty response")
                    }
                } else {
                    _weatherResult.value =
                        NetworkResponse.Error("Failed to get location${geoResponse.message()}")
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error("Failed to get location")
                e.printStackTrace()
            }
        }
    }

    private fun updateUserCity(city: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("user").document(uid).update("city", city)
            .addOnSuccessListener {
                Log.d("WeatherViewModel", "User  city updated successfully")
            }
            .addOnFailureListener { exception ->
                Log.d("WeatherViewModel", "Error updating city: ${exception.message}")
            }
        val batch = db.batch()
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", uid),
                Filter.equalTo("user2.userId", uid)
            )
        ).get().addOnSuccessListener { documents ->
            for (doc in documents) {
                val chatData = doc.toObject(ChatData::class.java)
                val chatRef = db.collection(CHATS).document(doc.id)
                if (chatData.user1.userId == uid) {
                    batch.update(chatRef, "user1.city", city)
                } else if (chatData.user2.userId == uid) {
                    batch.update(chatRef, "user2.city", city)
                }
            }
            batch.commit()
                .addOnSuccessListener {
                    Log.d("WeatherViewModel", "Chat user city updated successfully")
                }
                .addOnFailureListener { exception ->
                    Log.d("WeatherViewModel", "Error updating chat user city: ${exception.message}")
                }
        }.addOnFailureListener { exception ->
            Log.d("WeatherViewModel", "Error fetching chats: ${exception.message}")
        }
    }

    private fun getData(city: String) {
        _weatherResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApiInterface.getWeather(Constant.apikey, city)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _weatherResult.value = NetworkResponse.Success(it)
                    } ?: run {
                        _weatherResult.value =
                            NetworkResponse.Error("Failed to load: Empty weather data")
                    }
                } else {
                    _weatherResult.value =
                        NetworkResponse.Error("Failed to load weather: ${response.message()}")
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error("Failed to load weather: ${e.message}")
                e.printStackTrace()
            }
        }

    }
    private fun chatUserGetData(city: String) {
        _chatUserCityName.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApiInterface.getWeather(Constant.apikey, city)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _chatUserCityName.value = NetworkResponse.Success(it)
                    } ?: run {
                        _chatUserCityName.value =
                            NetworkResponse.Error("Failed to load: Empty weather data")
                    }
                } else {
                    _chatUserCityName.value =
                        NetworkResponse.Error("Failed to load weather: ${response.message()}")
                }
            } catch (e: Exception) {
                _chatUserCityName.value = NetworkResponse.Error("Failed to load weather: ${e.message}")
                e.printStackTrace()
            }
        }

    }

    fun fetchWeatherDataFromDatabase(city: String) {
        Log.d("WeatherViewModel", "fetchWeatherDataFromDatabase-$city")
        chatUserGetData(city)
    }
}
