package com.example.hellocompose.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.data.api.dto.QuizResponseDto
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.model.QuizConfig
import com.example.hellocompose.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        classDiscriminator = "type"
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.TypeMessage -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.UpdateSettings -> _state.update { it.copy(settings = intent.settings) }
            is ChatIntent.StartQuiz -> startQuiz(intent.config)
            is ChatIntent.SelectQuizOption -> selectOption(intent.optionKey, intent.optionText)
        }
    }

    private fun selectOption(optionKey: String, optionText: String) {
        // Кнопки блокируются через isLoading — quizData трогать не нужно,
        // иначе карточки вопроса потеряют данные и покажут сырой JSON
        val userMessage = ChatMessage(
            role = ChatMessage.Role.USER,
            content = "$optionKey: $optionText"
        )
        _state.update {
            it.copy(messages = it.messages + userMessage, isLoading = true)
        }
        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            val result = sendMessageUseCase(
                conversationHistory = _state.value.messages,
                settings = _state.value.settings
            )
            handleApiResult(result)
        }
    }

    private fun startQuiz(config: QuizConfig) {
        val quizSettings = ChatSettings(
            systemPrompt = buildQuizSystemPrompt(config),
            temperature = 0.7f,
            maxTokens = 500,
            topP = 0.9f,
            frequencyPenalty = 0.0f,
            presencePenalty = 0.0f
        )

        val userMessage = ChatMessage(
            role = ChatMessage.Role.USER,
            content = "Начни викторину! Задай первый вопрос."
        )

        _state.update {
            it.copy(
                messages = listOf(userMessage),
                inputText = "",
                isLoading = true,
                settings = quizSettings,
                quizActive = true
            )
        }

        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            val result = sendMessageUseCase(
                conversationHistory = _state.value.messages,
                settings = quizSettings
            )
            handleApiResult(result)
        }
    }

    private fun sendMessage() {
        val currentText = _state.value.inputText.trim()
        if (currentText.isBlank()) return

        val userMessage = ChatMessage(role = ChatMessage.Role.USER, content = currentText)
        _state.update {
            it.copy(messages = it.messages + userMessage, inputText = "", isLoading = true)
        }

        viewModelScope.launch {
            _effect.send(ChatEffect.ScrollToBottom)
            val result = sendMessageUseCase(
                conversationHistory = _state.value.messages,
                settings = _state.value.settings
            )
            handleApiResult(result)
        }
    }

    private suspend fun handleApiResult(result: Result<ChatMessage>) {
        result.onSuccess { raw ->
            val isQuiz = _state.value.quizActive
            val newMessages = if (isQuiz) {
                parseQuizMessages(raw)
            } else {
                listOf(raw)
            }
            _state.update {
                it.copy(messages = it.messages + newMessages, isLoading = false)
            }
            _effect.send(ChatEffect.ScrollToBottom)
        }.onFailure { error ->
            Log.e("ChatViewModel", "error=$error")
            val errorMessage = ChatMessage(
                role = ChatMessage.Role.ERROR,
                content = error.message ?: "Не удалось получить ответ"
            )
            _state.update {
                it.copy(messages = it.messages + errorMessage, isLoading = false)
            }
            _effect.send(ChatEffect.ScrollToBottom)
        }
    }

    /**
     * Парсит ответ AI в список ChatMessage.
     *
     * AI может вернуть:
     * - одиночный объект: { "type": "question", ... }
     * - массив из двух объектов: [{ "type": "answer", ... }, { "type": "question/final", ... }]
     *
     * Используем библиотечный полиморфный десериализатор kotlinx.serialization
     * через classDiscriminator = "type", настроенный в jsonParser.
     */
    private fun parseQuizMessages(raw: ChatMessage): List<ChatMessage> {
        return try {
            val jsonText = extractJsonBlock(raw.content)
            Log.d("QuizParser", "jsonText = $jsonText")

            // Парсим как JsonElement чтобы различить объект и массив
            val jsonElement = jsonParser.parseToJsonElement(jsonText)

            when (jsonElement) {
                is JsonArray -> {
                    // Массив — [answer, question] или [answer, final]
                    jsonElement.map { element ->
                        val dto = jsonParser.decodeFromJsonElement<QuizResponseDto>(element)
                        raw.copy(content = element.toString(), quizData = dto)
                    }
                }
                is JsonObject -> {
                    // Одиночный объект — { "type": "question", ... }
                    val dto = jsonParser.decodeFromJsonElement<QuizResponseDto>(jsonElement)
                    listOf(raw.copy(quizData = dto))
                }
                else -> {
                    Log.e("QuizParser", "Unexpected JSON element type: $jsonElement")
                    listOf(raw.copy(content = raw.content.trim()))
                }
            }
        } catch (e: Exception) {
            Log.e("QuizParser", "Failed to parse quiz JSON: ${e.message}\nRaw: ${raw.content}")
            listOf(raw.copy(content = raw.content.trim()))
        }
    }

    /**
     * Нормализует ответ AI к валидному JSON-строке.
     *
     * AI может вернуть:
     * 1. ```json { ... } ```          — markdown-блок с одним объектом
     * 2. ```json { } { } ```          — markdown-блок с двумя объектами (без массива)
     * 3. { ... }                      — чистый одиночный объект
     * 4. [ { }, { } ]                 — корректный массив
     * 5. { ... } { ... }              — два объекта подряд без обёртки массива
     *
     * Случаи 2 и 5 → оборачиваем в [ ... , ... ]
     */
    private fun extractJsonBlock(text: String): String {
        val trimmed = text.trim()

        // Шаг 1: вытащить содержимое из markdown-блока если есть
        val jsonBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val inner = jsonBlockRegex.find(trimmed)?.groupValues?.get(1)?.trim() ?: trimmed

        // Шаг 2: если уже валидный массив или одиночный объект — вернуть как есть
        if (inner.startsWith("[") || (inner.startsWith("{") && countTopLevelObjects(inner) == 1)) {
            return inner
        }

        // Шаг 3: несколько JSON-объектов подряд — собираем их в массив
        val objects = extractTopLevelObjects(inner)
        return if (objects.size > 1) "[${objects.joinToString(",")}]" else inner
    }

    /**
     * Считает количество JSON-объектов верхнего уровня в строке.
     * Используется чтобы не оборачивать одиночный { } в массив.
     */
    private fun countTopLevelObjects(text: String): Int {
        var count = 0
        var depth = 0
        var inString = false
        var escape = false
        for (ch in text) {
            if (escape) { escape = false; continue }
            if (ch == '\\' && inString) { escape = true; continue }
            if (ch == '"') { inString = !inString; continue }
            if (inString) continue
            when (ch) {
                '{' -> { if (depth == 0) count++; depth++ }
                '}' -> depth--
            }
        }
        return count
    }

    /**
     * Извлекает все JSON-объекты верхнего уровня из строки.
     * Корректно обрабатывает вложенные объекты и строки с экранированием.
     */
    private fun extractTopLevelObjects(text: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        text.forEachIndexed { i, ch ->
            if (escape) { escape = false; return@forEachIndexed }
            if (ch == '\\' && inString) { escape = true; return@forEachIndexed }
            if (ch == '"') { inString = !inString; return@forEachIndexed }
            if (inString) return@forEachIndexed
            when (ch) {
                '{' -> { if (depth == 0) start = i; depth++ }
                '}' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        result.add(text.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return result
    }

    private fun buildQuizSystemPrompt(config: QuizConfig): String = """
Ты — ведущий мини-викторины. Отвечай ТОЛЬКО валидным JSON без markdown, без пояснений, без текста вне JSON.

ПРАВИЛА:
- Всего ${config.questionCount} вопросов ${config.topic.prompt}
- Сложность: ${config.difficulty.prompt}
- Задавай ПО ОДНОМУ вопросу за раз

ФОРМАТ — вопрос (type=question):
{
  "type": "question",
  "question_number": <N>,
  "total": ${config.questionCount},
  "question": "<текст вопроса>",
  "options": { "A": "<вариант>", "B": "<вариант>", "C": "<вариант>", "D": "<вариант>" },
  "correct": "<A|B|C|D>"
}

ФОРМАТ — оценка ответа (type=answer), возвращай ВМЕСТЕ со следующим вопросом:
Если вопросы ещё остались — верни JSON-массив из двух объектов: [answer, question].
Если это был последний вопрос — верни JSON-массив из двух объектов: [answer, final].

ФОРМАТ — оценка ответа:
{
  "type": "answer",
  "correct": <true|false>,
  "correct_option": "<A|B|C|D>",
  "correct_text": "<текст правильного варианта>",
  "explanation": "<краткое объяснение, 1 предложение>",
  "score": <текущий счёт>,
  "total": ${config.questionCount}
}

ФОРМАТ — финальный итог (type=final):
{
  "type": "final",
  "score": <итоговый счёт>,
  "total": ${config.questionCount},
  "comment": "<короткий итоговый комментарий>"
}

ВАЖНО: отвечай ТОЛЬКО JSON, никакого другого текста.
    """.trimIndent()
}
