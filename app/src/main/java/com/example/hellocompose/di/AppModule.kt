package com.example.hellocompose.di

import androidx.room.Room
import com.example.hellocompose.data.api.ApiConstants
import com.example.hellocompose.data.api.DeepSeekApiService
import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.db.AgentDatabase
import com.example.hellocompose.data.repository.AgentHistoryRepository
import com.example.hellocompose.data.repository.ChatRepositoryImpl
import com.example.hellocompose.data.repository.ModelComparisonRepositoryImpl
import com.example.hellocompose.domain.agent.Agent
import com.example.hellocompose.domain.agent.tools.CalculatorTool
import com.example.hellocompose.domain.agent.tools.DateTimeTool
import org.koin.android.ext.koin.androidContext
import com.example.hellocompose.domain.repository.ChatRepository
import com.example.hellocompose.domain.repository.ModelComparisonRepository
import com.example.hellocompose.domain.usecase.CompareModelsUseCase
import com.example.hellocompose.domain.usecase.JudgeUseCase
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import com.example.hellocompose.presentation.ChatViewModel
import com.example.hellocompose.presentation.agent.AgentViewModel
import com.example.hellocompose.presentation.expert.ExpertChatViewModel
import com.example.hellocompose.presentation.modelcomparison.ModelComparisonViewModel
import com.example.hellocompose.presentation.temperature.TemperatureChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
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
                    explicitNulls = false  // null-поля не отправляются
                    encodeDefaults = true  // поля с дефолтами (type="function") отправляются
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
    viewModel { ExpertChatViewModel(get()) }
    viewModel { TemperatureChatViewModel(get()) }

    // Model Comparison
    single(named("deepseekComparison")) {
        ModelComparisonApiService(
            client = get(),
            baseUrl = ApiConstants.BASE_URL,
            apiKey = ApiConstants.API_KEY
        )
    }
    single(named("groqComparison")) {
        ModelComparisonApiService(
            client = get(),
            baseUrl = ApiConstants.GROQ_BASE_URL,
            apiKey = ApiConstants.GROQ_API_KEY
        )
    }
    single<ModelComparisonRepository> {
        ModelComparisonRepositoryImpl(
            deepSeekService = get(named("deepseekComparison")),
            groqService = get(named("groqComparison"))
        )
    }
    factory { CompareModelsUseCase(get()) }
    factory { JudgeUseCase(get(named("deepseekComparison"))) }
    viewModel { ModelComparisonViewModel(get(), get()) }

    // Agent (Day 7: Room persistence)
    single {
        Room.databaseBuilder(androidContext(), AgentDatabase::class.java, "agent-db")
            .build()
    }
    single { get<AgentDatabase>().agentMessageDao() }
    single { AgentHistoryRepository(get()) }
    single { Agent(get(named("deepseekComparison")), listOf(DateTimeTool(), CalculatorTool()), get()) }
    viewModel { AgentViewModel(get()) }
}
