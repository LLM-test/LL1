package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.db.AgentContextDao
import com.example.hellocompose.data.db.AgentContextEntity
import com.example.hellocompose.data.db.AgentFactDao
import com.example.hellocompose.data.db.AgentFactEntity
import com.example.hellocompose.data.db.AgentMessageDao
import com.example.hellocompose.data.db.toEntity
import com.example.hellocompose.data.db.toMessageDto

/**
 * Репозиторий для сохранения и загрузки данных агента через Room.
 *
 * День 7: история сообщений.
 * День 9: резюме (summary) и coveredCount.
 * День 10: ключевые факты для стратегии Sticky Facts.
 */
class AgentHistoryRepository(
    private val messageDao: AgentMessageDao,
    private val contextDao: AgentContextDao,
    private val factDao: AgentFactDao
) {

    // ── Сообщения ─────────────────────────────────────────────────────────────

    suspend fun loadHistory(): List<MessageDto> =
        messageDao.getAll().map { it.toMessageDto() }

    suspend fun saveMessage(message: MessageDto) =
        messageDao.insert(message.toEntity())

    /** Очищает историю, резюме и факты. */
    suspend fun clearHistory() {
        messageDao.deleteAll()
        contextDao.clear()
        factDao.clear()
    }

    // ── Резюме (Day 9) ────────────────────────────────────────────────────────

    suspend fun loadContext(): Pair<String, Int> {
        val ctx = contextDao.get() ?: return "" to 0
        return ctx.summary to ctx.coveredCount
    }

    suspend fun saveContext(summary: String, coveredCount: Int) =
        contextDao.save(AgentContextEntity(summary = summary, coveredCount = coveredCount))

    // ── Факты (Day 10 / Sticky Facts) ────────────────────────────────────────

    suspend fun loadFacts(): Map<String, String> =
        factDao.getAll().associate { it.key to it.value }

    suspend fun saveFact(key: String, value: String) =
        factDao.upsert(AgentFactEntity(key = key, value = value))

    suspend fun clearFacts() = factDao.clear()
}
