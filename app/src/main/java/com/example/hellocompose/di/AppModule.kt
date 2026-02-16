package com.example.hellocompose.di

import com.example.hellocompose.data.api.DeepSeekApiService
import com.example.hellocompose.data.repository.ChatRepositoryImpl
import com.example.hellocompose.domain.repository.ChatRepository
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import com.example.hellocompose.presentation.ChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single {
        HttpClient(Android) {
            engine {
                connectTimeout = 30_000
                socketTimeout = 60_000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    single { DeepSeekApiService(get()) }

    single<ChatRepository> { ChatRepositoryImpl(get()) }

    factory { SendMessageUseCase(get()) }

    viewModel { ChatViewModel(get()) }
}
