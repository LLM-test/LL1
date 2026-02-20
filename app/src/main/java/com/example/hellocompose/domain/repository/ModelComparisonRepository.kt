package com.example.hellocompose.domain.repository

import com.example.hellocompose.domain.model.ModelComparisonResponse
import com.example.hellocompose.domain.model.ModelConfig

interface ModelComparisonRepository {
    suspend fun sendToModel(
        question: String,
        modelConfig: ModelConfig
    ): ModelComparisonResponse
}
