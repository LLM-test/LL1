package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.ModelComparisonApiService
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.domain.model.ApiProvider
import com.example.hellocompose.domain.model.ModelComparisonResponse
import com.example.hellocompose.domain.model.ModelConfig
import com.example.hellocompose.domain.repository.ModelComparisonRepository

class ModelComparisonRepositoryImpl(
    private val deepSeekService: ModelComparisonApiService,
    private val groqService: ModelComparisonApiService
) : ModelComparisonRepository {

    override suspend fun sendToModel(
        question: String,
        modelConfig: ModelConfig
    ): ModelComparisonResponse {
        val service = when (modelConfig.apiProvider) {
            ApiProvider.DEEPSEEK -> deepSeekService
            ApiProvider.GROQ     -> groqService
        }
        val request = ChatRequestDto(
            model = modelConfig.modelName,
            messages = listOf(MessageDto(role = "user", content = question)),
            temperature = modelConfig.temperature,   // null для Reasoner — параметр не передаётся
            maxTokens = modelConfig.maxTokens
        )

        val startMs = System.currentTimeMillis()
        val response = service.chatCompletions(request)
        val elapsedMs = System.currentTimeMillis() - startMs

        val usage = response.usage
        val promptTokens = usage?.promptTokens ?: 0
        val completionTokens = usage?.completionTokens ?: 0
        val costUsd = (promptTokens * modelConfig.inputCostPerMillion +
                completionTokens * modelConfig.outputCostPerMillion) / 1_000_000.0

        return ModelComparisonResponse(
            modelConfig = modelConfig,
            content = response.choices.firstOrNull()?.message?.content
                ?: error("Пустой ответ от ${modelConfig.displayName}"),
            isLoading = false,
            elapsedMs = elapsedMs,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            costUsd = costUsd
        )
    }
}
