package com.example.bands.data

open class Event<out T>(val content :T) {
    private var isExceptionHandled =false
    fun getContentOrNull(): T?{
        return if (isExceptionHandled)null else {
            isExceptionHandled=true
            content
        }
    }
}