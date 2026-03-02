package com.example.hellocompose.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Один ключевой факт, извлечённый агентом в стратегии Sticky Facts.
 * Первичный ключ — сам ключ факта (upsert при обновлении).
 */
@Entity(tableName = "agent_facts")
data class AgentFactEntity(
    @PrimaryKey val key: String,
    val value: String
)
