package com.example.bands.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bands.data.api.Constant
import com.example.bands.data.api.WeatherModel
import com.example.bands.weatherupdates.GeolocationApi
import com.example.bands.weatherupdates.NetworkResponse
import com.example.bands.weatherupdates.WeatherApiInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherApiInterface: WeatherApiInterface,
    private val geolocationApi: GeolocationApi
):ViewModel() {
    private val _weatherResult = MutableLiveData<NetworkResponse<WeatherModel>>()
    val weatherResult: LiveData<NetworkResponse<WeatherModel>> = _weatherResult

    init {
        getCityFromIpAndFetchWeather()
    }
    private fun  getCityFromIpAndFetchWeather(){
        viewModelScope.launch {
            try {
                val geoResponse= geolocationApi.getLocationData()
                if (geoResponse.isSuccessful){
                    geoResponse.body()?.let {location ->
                        val cityName=location.city
                        getData(cityName)
                    }?:run {
                        _weatherResult.value = NetworkResponse.Error("Failed to load: Empty response")
                    }
                }else{
                    _weatherResult.value = NetworkResponse.Error("Failed to get location${geoResponse.message()}")
                }
            }catch (e:Exception){
                _weatherResult.value = NetworkResponse.Error("Failed to get location")
                e.printStackTrace()
            }
        }
    }
    private fun getData(city:String){
        _weatherResult.value=NetworkResponse.Loading
        viewModelScope.launch {
            try {
                val response =weatherApiInterface.getWeather(Constant.apikey,city)
                if (response.isSuccessful){
                    response.body()?.let {
                        _weatherResult.value = NetworkResponse.Success(it)
                    }?:run {
                        _weatherResult.value = NetworkResponse.Error("Failed to load: Empty weather data")
                    }
                }else{
                    _weatherResult.value=NetworkResponse.Error("Failed to load weather: ${response.message()}")
                }
            }catch (e:Exception){
                _weatherResult.value=NetworkResponse.Error("Failed to load weather: ${e.message}")
                e.printStackTrace()
            }
        }

    }
}