package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.db.AgentContextDao
import com.example.hellocompose.data.db.AgentContextEntity
import com.example.hellocompose.data.db.AgentMessageDao
import com.example.hellocompose.data.db.toEntity
import com.example.hellocompose.data.db.toMessageDto

/**
 * Репозиторий для сохранения и загрузки истории диалога агента через Room.
 *
 * День 9: добавлено хранение сжатого резюме (summary) и индекса охваченных сообщений.
 */
class AgentHistoryRepository(
    private val messageDao: AgentMessageDao,
    private val contextDao: AgentContextDao
) {

    // ── Сообщения ─────────────────────────────────────────────────────────────

    suspend fun loadHistory(): List<MessageDto> =
        messageDao.getAll().map { it.toMessageDto() }

    suspend fun saveMessage(message: MessageDto) =
        messageDao.insert(message.toEntity())

    /** Очищает и историю сообщений, и сохранённое резюме. */
    suspend fun clearHistory() {
        messageDao.deleteAll()
        contextDao.clear()
    }

    // ── Резюме (summary) ──────────────────────────────────────────────────────

    /**
     * Загружает сохранённое резюме и количество охваченных сообщений.
     * Возвращает ("", 0) если резюме ещё не создавалось.
     */
    suspend fun loadContext(): Pair<String, Int> {
        val ctx = contextDao.get() ?: return "" to 0
        return ctx.summary to ctx.coveredCount
    }

    /** Сохраняет обновлённое резюме и количество охваченных сообщений. */
    suspend fun saveContext(summary: String, coveredCount: Int) =
        contextDao.save(AgentContextEntity(summary = summary, coveredCount = coveredCount))
}
