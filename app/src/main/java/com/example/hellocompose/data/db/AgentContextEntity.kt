package com.example.hellocompose.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Хранит сжатое резюме диалога агента и количество сообщений, которые оно покрывает.
 * Всегда одна строка с id = 1 (upsert при сохранении).
 */
@Entity(tableName = "agent_context")
data class AgentContextEntity(
    @PrimaryKey val id: Int = 1,
    val summary: String = "",
    @ColumnInfo(name = "covered_count") val coveredCount: Int = 0
)
