package com.example.hellocompose.domain.memory

import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    /** Реактивный поток всех записей обоих слоёв. */
    fun getAll(): Flow<List<MemoryEntry>>

    /** Реактивный поток записей конкретного слоя. */
    fun getByType(type: MemoryType): Flow<List<MemoryEntry>>

    /** Сохранить новую запись. */
    suspend fun save(type: MemoryType, key: String, value: String)

    /** Удалить запись по id. */
    suspend fun delete(id: Long)

    /** Очистить конкретный слой. */
    suspend fun clearByType(type: MemoryType)

    /**
     * Формирует блок текста для вставки в system prompt агента.
     * Возвращает пустую строку, если память пуста.
     *
     * Пример:
     * ```
     * === ПАМЯТЬ АССИСТЕНТА ===
     * [ДОЛГОВРЕМЕННАЯ ПАМЯТЬ]
     * profile.name: Андрей
     * profile.expertise: Android Kotlin/Compose
     * [РАБОЧАЯ ПАМЯТЬ]
     * task.name: Трекер привычек
     * task.goal: Реализовать напоминания
     * ```
     */
    suspend fun getMemoryPrompt(): String
}
