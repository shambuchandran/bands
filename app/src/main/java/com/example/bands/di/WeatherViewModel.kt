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
            Log.d("WeatherApp", "Starting getCityFromIpAndFetchWeather function")
            try {
                val geoResponse = geolocationApi.getLocationData()
                Log.d("WeatherApp", "Received geolocation API response: ${geoResponse.code()} - ${geoResponse.message()}")
                if (geoResponse.isSuccessful) {
                    geoResponse.body()?.let { location ->
                        Log.d("WeatherApp", "Retrieved city name from geolocation API: ${location.city}")
                        val cityName = location.city
                        getData(cityName)
                    } ?: run {
                        _weatherResult.value =
                            NetworkResponse.Error("Failed to load: Empty response")
                        Log.e("WeatherApp", "Error: Empty response from geolocation API")
                    }
                } else {
                    _weatherResult.value =
                        NetworkResponse.Error("Failed to get location${geoResponse.message()}")
                    Log.e("WeatherApp", "Failed to get location data. Response code: ${geoResponse.code()}, Message: ${geoResponse.message()}")
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error("Failed to get location")
                Log.e("WeatherApp", "Exception occurred while fetching location", e)
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
    private fun handleCityFetchFailure() {
        fetchLastSavedCity { lastSavedCity ->
            if (lastSavedCity != null) {
                Log.d("WeatherApp", "Using last saved city: $lastSavedCity")
                getData(lastSavedCity)
                Log.e("WeatherApp", "No saved city available.")
            }
        }
    }
    private fun fetchLastSavedCity(onResult: (String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(null)
            return
        }
        db.collection("user").document(uid).get()
            .addOnSuccessListener { document ->
                val lastSavedCity = document.getString("city")
                onResult(lastSavedCity)
            }
            .addOnFailureListener { exception ->
                Log.e("WeatherViewModel", "Error fetching last saved city: ${exception.message}")
                onResult(null)
            }
    }

    private fun getData(city: String) {
        Log.d("WeatherApp", "getData() called with city: $city")
        _weatherResult.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApiInterface.getWeather(Constant.apikey, city)
                Log.d("WeatherApp", "Weather API response received: ${response.code()} - ${response.message()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        _weatherResult.value = NetworkResponse.Success(it)
                        updateUserCity(city)
                        Log.d("WeatherApp", "Successfully retrieved weather data: $it")
                    } ?: run {
                        _weatherResult.value = NetworkResponse.Error("Failed to load: Empty weather data")
                        Log.e("WeatherApp", "Failed to load: Empty weather data")
                        handleCityFetchFailure()
                    }
                } else {
                    _weatherResult.value =
                        NetworkResponse.Error("Failed to load weather: ${response.message()}")
                    Log.e("WeatherApp", "Failed to load weather: ${response.message()}")
                    handleCityFetchFailure()
                }
            } catch (e: Exception) {
                _weatherResult.value = NetworkResponse.Error("Failed to load weather: ${e.message}")
                handleCityFetchFailure()
                Log.e("WeatherApp", "Exception occurred while loading weather data: ${e.message}", e)
                e.printStackTrace()
            }
        }

    }
    private fun chatUserGetData(city: String) {
        _chatUserCityName.value = NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response = weatherApiInterface.getWeather(Constant.apikey, city)
                Log.d("WeatherApp", "Weather API response received for chat user: ${response.code()} - ${response.message()}")
                if (response.isSuccessful) {
                    response.body()?.let {
                        _chatUserCityName.value = NetworkResponse.Success(it)
                        Log.d("WeatherApp", "Successfully retrieved weather data for chat user: $it")
                    } ?: run {
                        _chatUserCityName.value =
                            NetworkResponse.Error("Failed to load: Empty weather data")
                        Log.e("WeatherApp", "Failed to load weather data: Empty weather data for chat user")
                    }
                } else {
                    _chatUserCityName.value =
                        NetworkResponse.Error("Failed to load weather: ${response.message()}")
                    Log.e("WeatherApp", "Failed to load weather data for chat user: ${response.message()}")
                }
            } catch (e: Exception) {
                _chatUserCityName.value = NetworkResponse.Error("Failed to load weather: ${e.message}")
                Log.e("WeatherApp", "Exception occurred while loading weather data for chat user: ${e.message}", e)
                e.printStackTrace()
            }
        }

    }

    fun fetchWeatherDataFromDatabase(city: String) {
        Log.d("WeatherViewModel", "fetchWeatherDataFromDatabase-$city")
        chatUserGetData(city)
    }
}
