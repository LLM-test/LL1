package com.example.hellocompose.domain.model

/**
 * Метаданные модели для экрана сравнения.
 * Стоимость указана в USD за миллион токенов.
 */
data class ModelConfig(
    val id: String,
    val modelName: String,
    val displayName: String,
    val emoji: String,
    val tierLabel: String,
    val accentColor: Long,
    val apiProvider: ApiProvider,
    val inputCostPerMillion: Double,
    val outputCostPerMillion: Double,
    /** null = не передавать параметр в API (например, deepseek-reasoner не поддерживает) */
    val temperature: Float? = 0.7f,
    val maxTokens: Int = 1000
)

enum class ApiProvider { DEEPSEEK, GROQ }

object ModelConfigs {
    val GROQ_LLAMA_8B = ModelConfig(
        id = "llama-3.1-8b-instant",
        modelName = "llama-3.1-8b-instant",
        displayName = "Llama 3.1 8B",
        emoji = "🐥",
        tierLabel = "слабая / молниеносная",
        accentColor = 0xFF6A1B9A,
        apiProvider = ApiProvider.GROQ,
        inputCostPerMillion = 0.05,
        outputCostPerMillion = 0.08
    )
    val DEEPSEEK_CHAT = ModelConfig(
        id = "deepseek-chat",
        modelName = "deepseek-chat",
        displayName = "DeepSeek Chat",
        emoji = "🦙",
        tierLabel = "средняя / медленная",
        accentColor = 0xFF1565C0,
        apiProvider = ApiProvider.DEEPSEEK,
        inputCostPerMillion = 0.14,
        outputCostPerMillion = 0.28
    )
    val GROQ_LLAMA_70B = ModelConfig(
        id = "llama-3.3-70b-versatile",
        modelName = "llama-3.3-70b-versatile",
        displayName = "Llama 3.3 70B",
        emoji = "🦬",
        tierLabel = "сильная / быстрая",
        accentColor = 0xFF1B5E20,
        apiProvider = ApiProvider.GROQ,
        inputCostPerMillion = 0.59,
        outputCostPerMillion = 0.79
    )
    // Только судья — не участвует в сравнении
    val DEEPSEEK_REASONER = ModelConfig(
        id = "deepseek-reasoner",
        modelName = "deepseek-reasoner",
        displayName = "DeepSeek Reasoner",
        emoji = "🧠",
        tierLabel = "судья",
        accentColor = 0xFFBF360C,
        apiProvider = ApiProvider.DEEPSEEK,
        inputCostPerMillion = 0.55,
        outputCostPerMillion = 2.19,
        temperature = null,
        maxTokens = 2000
    )
    val ALL = listOf(GROQ_LLAMA_8B, DEEPSEEK_CHAT, GROQ_LLAMA_70B)
}
