package com.example.bands.weatherupdates

import com.example.bands.data.api.GeolocationResponse
import com.example.bands.data.api.WeatherModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiInterface {
    @GET("/v1/current.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") city: String,
    ): Response<WeatherModel>
}

interface GeolocationApi {
    @GET("json")
    suspend fun getLocationData():Response<GeolocationResponse>
}