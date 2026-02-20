package com.example.hellocompose.domain.usecase

import com.example.hellocompose.domain.model.ModelComparisonResponse
import com.example.hellocompose.domain.model.ModelConfig
import com.example.hellocompose.domain.repository.ModelComparisonRepository

class CompareModelsUseCase(private val repository: ModelComparisonRepository) {
    suspend operator fun invoke(
        question: String,
        modelConfig: ModelConfig
    ): Result<ModelComparisonResponse> = runCatching {
        repository.sendToModel(question, modelConfig)
    }
}
