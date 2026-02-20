package com.example.hellocompose.domain.model

data class ModelComparisonRound(
    val id: Long = System.currentTimeMillis(),
    val question: String,
    val responses: List<ModelComparisonResponse> = emptyList(),
    val judgeVerdict: JudgeVerdict? = null
)

data class JudgeVerdict(
    val content: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

data class ModelComparisonResponse(
    val modelConfig: ModelConfig,
    val content: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val elapsedMs: Long = 0L,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val costUsd: Double = 0.0
)
