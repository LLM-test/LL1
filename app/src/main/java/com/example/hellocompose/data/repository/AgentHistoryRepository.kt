package com.example.hellocompose.data.repository

import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.db.AgentMessageDao
import com.example.hellocompose.data.db.toEntity
import com.example.hellocompose.data.db.toMessageDto

/**
 * Репозиторий для сохранения и загрузки истории диалога агента через Room.
 */
class AgentHistoryRepository(private val dao: AgentMessageDao) {

    suspend fun loadHistory(): List<MessageDto> =
        dao.getAll().map { it.toMessageDto() }

    suspend fun saveMessage(message: MessageDto) =
        dao.insert(message.toEntity())

    suspend fun clearHistory() =
        dao.deleteAll()
}
