package com.example.bands.di

import com.example.bands.data.gemApiKey
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
}