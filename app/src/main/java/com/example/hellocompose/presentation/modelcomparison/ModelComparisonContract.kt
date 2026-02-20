package com.example.hellocompose.presentation.modelcomparison

import com.example.hellocompose.domain.model.ModelComparisonRound

data class ModelComparisonState(
    val rounds: List<ModelComparisonRound> = emptyList(),
    val inputText: String = "",
    val isAnyLoading: Boolean = false
)

sealed interface ModelComparisonIntent {
    data class TypeMessage(val text: String) : ModelComparisonIntent
    data object SendMessage : ModelComparisonIntent
    data object ClearHistory : ModelComparisonIntent
}

sealed interface ModelComparisonEffect {
    data object ScrollToBottom : ModelComparisonEffect
}
