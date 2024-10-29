package com.example.bands.di

import com.example.bands.data.gemApiKey
import com.example.bands.weatherupdates.GeolocationApi
import com.example.bands.weatherupdates.WeatherApiInterface
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(ViewModelComponent::class)
class HiltModule {

    @Provides
    fun providesAuth():FirebaseAuth = Firebase.auth

    @Provides
    fun providesFireStore():FirebaseFirestore =Firebase.firestore

    @Provides
    fun providesStorage(): FirebaseStorage= Firebase.storage

    @Provides
    @ViewModelScoped
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(modelName = "gemini-1.5-flash", apiKey = gemApiKey)
    }

    private val WEATHER_API_BASE_URL = "https://api.weatherapi.com"
    private val GEOLOCATION_API_BASE_URL = "https://ipinfo.io/"
    @Provides
    @ViewModelScoped
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    @Provides
    @ViewModelScoped
    fun provideWeatherApi(okHttpClient: OkHttpClient): WeatherApiInterface {
        return Retrofit.Builder()
            .baseUrl(WEATHER_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiInterface::class.java)
    }
    @Provides
    @ViewModelScoped
    fun provideGeolocationApi(okHttpClient: OkHttpClient): GeolocationApi {
        return Retrofit.Builder()
            .baseUrl(GEOLOCATION_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeolocationApi::class.java)
    }
}