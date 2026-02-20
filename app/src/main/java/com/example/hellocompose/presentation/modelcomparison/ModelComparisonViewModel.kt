package com.example.hellocompose.presentation.modelcomparison

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.model.JudgeVerdict
import com.example.hellocompose.domain.model.ModelComparisonResponse
import com.example.hellocompose.domain.model.ModelComparisonRound
import com.example.hellocompose.domain.model.ModelConfigs
import com.example.hellocompose.domain.usecase.CompareModelsUseCase
import com.example.hellocompose.domain.usecase.JudgeUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModelComparisonViewModel(
    private val compareModelsUseCase: CompareModelsUseCase,
    private val judgeUseCase: JudgeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ModelComparisonState())
    val state: StateFlow<ModelComparisonState> = _state.asStateFlow()

    private val _effect = Channel<ModelComparisonEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: ModelComparisonIntent) {
        when (intent) {
            is ModelComparisonIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is ModelComparisonIntent.SendMessage -> sendMessage()
            is ModelComparisonIntent.ClearHistory -> _state.update {
                it.copy(rounds = emptyList(), inputText = "")
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isAnyLoading) return

        // Создаём раунд с loading-заглушками для каждой модели
        val loadingResponses = ModelConfigs.ALL.map { cfg ->
            ModelComparisonResponse(modelConfig = cfg, isLoading = true)
        }
        val round = ModelComparisonRound(question = text, responses = loadingResponses)
        val roundId = round.id

        _state.update {
            it.copy(rounds = it.rounds + round, inputText = "", isAnyLoading = true)
        }

        viewModelScope.launch {
            _effect.send(ModelComparisonEffect.ScrollToBottom)

            // Параллельно запускаем три запроса — по одному на каждую модель
            val deferred = ModelConfigs.ALL.map { cfg ->
                async {
                    val result = compareModelsUseCase(question = text, modelConfig = cfg)
                    cfg to result
                }
            }

            val results = deferred.map { it.await() }

            // Собираем финальные ответы трёх моделей
            val finalResponses = results.map { (cfg, result) ->
                result.fold(
                    onSuccess = { it },
                    onFailure = { error ->
                        Log.e("ModelComparison", "Error for ${cfg.id}: ${error.message}")
                        ModelComparisonResponse(
                            modelConfig = cfg,
                            content = error.message ?: "Ошибка",
                            isLoading = false,
                            isError = true
                        )
                    }
                )
            }

            // Обновляем ответы и сразу показываем loading у судьи
            _state.update { state ->
                val updatedRounds = state.rounds.map { r ->
                    if (r.id != roundId) return@map r
                    r.copy(
                        responses = finalResponses,
                        judgeVerdict = JudgeVerdict(isLoading = true)
                    )
                }
                state.copy(rounds = updatedRounds, isAnyLoading = false)
            }

            _effect.send(ModelComparisonEffect.ScrollToBottom)

            // 4-й запрос: судья оценивает все три ответа
            val successfulResponses = finalResponses.filter { !it.isError && it.content.isNotBlank() }
            val judgeResult = judgeUseCase(question = text, responses = successfulResponses)

            _state.update { state ->
                val updatedRounds = state.rounds.map { r ->
                    if (r.id != roundId) return@map r
                    val verdict = judgeResult.fold(
                        onSuccess = { JudgeVerdict(content = it, isLoading = false) },
                        onFailure = { error ->
                            Log.e("ModelComparison", "Judge error: ${error.message}")
                            JudgeVerdict(content = error.message ?: "Ошибка судьи", isError = true)
                        }
                    )
                    r.copy(judgeVerdict = verdict)
                }
                state.copy(rounds = updatedRounds)
            }

            _effect.send(ModelComparisonEffect.ScrollToBottom)
        }
    }
}
