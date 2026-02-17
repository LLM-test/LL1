package com.example.hellocompose.data.api

import com.example.hellocompose.BuildConfig

object ApiConstants {
    const val BASE_URL = "https://api.deepseek.com/"
    val API_KEY: String get() = BuildConfig.DEEPSEEK_API_KEY
    const val MODEL = "deepseek-chat"
}
