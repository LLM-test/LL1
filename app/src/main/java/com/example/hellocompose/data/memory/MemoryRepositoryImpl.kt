package com.example.hellocompose.data.memory

import com.example.hellocompose.domain.memory.MemoryEntry
import com.example.hellocompose.domain.memory.MemoryRepository
import com.example.hellocompose.domain.memory.MemoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepositoryImpl(private val dao: MemoryDao) : MemoryRepository {

    override fun getAll(): Flow<List<MemoryEntry>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByType(type: MemoryType): Flow<List<MemoryEntry>> =
        dao.getByType(type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun save(type: MemoryType, key: String, value: String) {
        dao.insert(MemoryEntryEntity(type = type.name, key = key.trim(), value = value.trim()))
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun clearByType(type: MemoryType) = dao.clearByType(type.name)

    /**
     * Строит текстовый блок для вставки в system prompt:
     *
     * ```
     * === ПАМЯТЬ АССИСТЕНТА ===
     * [ДОЛГОВРЕМЕННАЯ ПАМЯТЬ]
     * profile.name: Андрей
     * [РАБОЧАЯ ПАМЯТЬ]
     * task.name: Трекер привычек
     * ```
     */
    override suspend fun getMemoryPrompt(): String {
        val entries = dao.getAllSync().map { it.toDomain() }
        if (entries.isEmpty()) return ""

        return buildString {
            appendLine("=== ПАМЯТЬ АССИСТЕНТА ===")

            val longTerm = entries.filter { it.type == MemoryType.LONG_TERM }
            if (longTerm.isNotEmpty()) {
                appendLine("[ДОЛГОВРЕМЕННАЯ ПАМЯТЬ]")
                longTerm.forEach { appendLine("${it.key}: ${it.value}") }
            }

            val working = entries.filter { it.type == MemoryType.WORKING }
            if (working.isNotEmpty()) {
                appendLine("[РАБОЧАЯ ПАМЯТЬ]")
                working.forEach { appendLine("${it.key}: ${it.value}") }
            }
        }.trimEnd()
    }

    private fun MemoryEntryEntity.toDomain() = MemoryEntry(
        id        = id,
        type      = MemoryType.valueOf(type),
        key       = key,
        value     = value,
        createdAt = createdAt
    )
}
