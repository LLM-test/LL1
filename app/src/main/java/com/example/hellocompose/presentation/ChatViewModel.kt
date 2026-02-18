package com.example.hellocompose.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.data.api.dto.QuizResponseDto
import com.example.hellocompose.domain.model.ChatMessage
import com.example.hellocompose.domain.model.ChatSettings
import com.example.hellocompose.domain.model.QuizConfig
import com.example.hellocompose.domain.model.QuizTopic
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

    /**
     * Пользователь выбрал вариант ответа.
     * Оцениваем правильность локально (сравниваем с question.correct),
     * обновляем сообщение с вопросом (добавляем selectedOption),
     * увеличиваем счёт если верно, затем запрашиваем следующий вопрос у AI.
     */
    private fun selectOption(optionKey: String, @Suppress("UNUSED_PARAMETER") optionText: String) {
        val messages = _state.value.messages

        // Находим индекс последнего вопроса
        val questionIndex = messages.indexOfLast { it.quizData is QuizResponseDto.Question }
        if (questionIndex == -1) return

        val questionMessage = messages[questionIndex]
        val question = questionMessage.quizData as QuizResponseDto.Question

        // Локальная оценка
        val isCorrect = optionKey == question.correct
        val newScore = if (isCorrect) _state.value.localScore + 1 else _state.value.localScore

        // Отмечаем выбранный вариант на карточке вопроса
        val updatedMessages = messages.toMutableList().also { list ->
            list[questionIndex] = questionMessage.copy(selectedOption = optionKey)
        }

        _state.update {
            it.copy(
                messages = updatedMessages,
                isLoading = true,
                localScore = newScore
            )
        }

        // Сообщение для AI — просто просим следующий вопрос.
        // Передаём счёт чтобы AI мог корректно сформировать final.
        val isLastQuestion = question.questionNumber >= question.total
        val userContent = if (isLastQuestion) {
            "Это был последний вопрос. Мой итоговый счёт: $newScore/${question.total}. Верни финальный результат."
        } else {
            "Следующий вопрос (${question.questionNumber + 1}/${question.total})."
        }

        val userMessage = ChatMessage(
            role = ChatMessage.Role.USER,
            content = userContent
        )
        _state.update {
            it.copy(messages = it.messages + userMessage)
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
            temperature = config.temperature,
            maxTokens = config.maxTokens,
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
                quizActive = true,
                localScore = 0,
                totalQuestions = config.questionCount
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
            // Если пришёл final — деактивируем quiz
            val hasFinal = newMessages.any { it.quizData is QuizResponseDto.Final }
            _state.update {
                it.copy(
                    messages = it.messages + newMessages,
                    isLoading = false,
                    quizActive = if (hasFinal) false else it.quizActive
                )
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
     * AI возвращает только type=question или type=final.
     * Одиночный объект: { "type": "question", ... }
     * Массив (на всякий случай): [{ "type": "question", ... }]
     */
    private fun parseQuizMessages(raw: ChatMessage): List<ChatMessage> {
        return try {
            val jsonText = extractJsonBlock(raw.content)
            Log.d("QuizParser", "jsonText = $jsonText")

            val jsonElement = jsonParser.parseToJsonElement(jsonText)

            when (jsonElement) {
                is JsonArray -> {
                    jsonElement.map { element ->
                        val dto = jsonParser.decodeFromJsonElement<QuizResponseDto>(element)
                        raw.copy(content = element.toString(), quizData = dto)
                    }
                }
                is JsonObject -> {
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

    private fun buildQuizSystemPrompt(config: QuizConfig): String {
        return if (config.topic == QuizTopic.RUSSIAN) {
            buildRussianQuizPrompt(config)
        } else {
            buildGeneralQuizPrompt(config)
        }
    }

    private fun buildGeneralQuizPrompt(config: QuizConfig): String = """
Ты — ведущий мини-викторины. Отвечай ТОЛЬКО валидным JSON без markdown, без пояснений, без текста вне JSON.

ПРАВИЛА:
- Всего ${config.questionCount} вопросов ${config.topic.prompt}${if (config.subtopic.isNotBlank()) ", конкретная подтема: «${config.subtopic}»" else ""}
- Сложность: ${config.difficulty.prompt}
- Задавай ПО ОДНОМУ вопросу за раз
- Правильность ответов оценивается на стороне приложения — тебе не нужно возвращать type=answer
- Когда пользователь просит следующий вопрос — просто верни следующий вопрос
- Когда пользователь просит финальный результат — верни type=final

ФОРМАТ — вопрос (type=question):
{
  "type": "question",
  "question_number": <N>,
  "total": ${config.questionCount},
  "question": "<текст вопроса>",
  "options": { "A": "<вариант>", "B": "<вариант>", "C": "<вариант>", "D": "<вариант>" },
  "correct": "<A|B|C|D>",
  "explanation": "<краткое объяснение правильного ответа, 1 предложение>"
}

ФОРМАТ — финальный итог (type=final):
{
  "type": "final",
  "score": <счёт из сообщения пользователя>,
  "total": ${config.questionCount},
  "comment": "<короткий итоговый комментарий>"
}

ВАЖНО: отвечай ТОЛЬКО JSON, никакого другого текста.
    """.trimIndent()

    private fun buildRussianQuizPrompt(config: QuizConfig): String = """
Ты — ведущий викторины по русскому языку. Отвечай ТОЛЬКО валидным JSON без markdown, без пояснений, без текста вне JSON.

ПРАВИЛА:
- Всего ${config.questionCount} вопросов${if (config.subtopic.isNotBlank()) " по теме «${config.subtopic}»" else " по разным темам (орфография, пунктуация, грамматика, лексика)"}
- Сложность: ${config.difficulty.prompt}
- Задавай ПО ОДНОМУ вопросу за раз
- Правильность ответов оценивается на стороне приложения — тебе не нужно возвращать type=answer
- Когда пользователь просит следующий вопрос — просто верни следующий вопрос
- Когда пользователь просит финальный результат — верни type=final

ВАЖНЫЕ ПРАВИЛА ДЛЯ ФОРМАТА ВОПРОСОВ:
- Для орфографии: показывай слово с пропуском буквы/букв в виде __, варианты — только вставляемые буквы или буквосочетания.
  Пример вопроса: "Вставьте пропущенную букву: пр__образование"
  Пример вариантов: A: "е", B: "и", C: "ы", D: "а"
- Для пунктуации: показывай предложение с пропущенным знаком в виде __, варианты — названия знаков или "знак не нужен".
  Пример вопроса: "Расставьте знак: Он пришёл__ когда уже стемнело."
  Пример вариантов: A: "запятая", B: "тире", C: "двоеточие", D: "знак не нужен"
- Для грамматики и теории: задавай прямой вопрос о правиле, варианты — короткие формулировки правил или примеры.
- НЕЛЬЗЯ давать варианты в виде готовых слов/предложений с уже расставленными буквами/знаками — правильный ответ не должен быть очевиден из написания варианта.

ФОРМАТ — вопрос (type=question):
{
  "type": "question",
  "question_number": <N>,
  "total": ${config.questionCount},
  "question": "<текст вопроса с __ вместо пропуска>",
  "options": { "A": "<только вставляемая часть>", "B": "<только вставляемая часть>", "C": "<только вставляемая часть>", "D": "<только вставляемая часть>" },
  "correct": "<A|B|C|D>",
  "explanation": "<краткое объяснение правила, 1 предложение>"
}

ФОРМАТ — финальный итог (type=final):
{
  "type": "final",
  "score": <счёт из сообщения пользователя>,
  "total": ${config.questionCount},
  "comment": "<короткий итоговый комментарий>"
}

ВАЖНО: отвечай ТОЛЬКО JSON, никакого другого текста.
    """.trimIndent()
}
