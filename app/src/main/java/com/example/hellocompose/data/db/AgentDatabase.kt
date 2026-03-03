package com.example.hellocompose.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hellocompose.data.memory.MemoryDao
import com.example.hellocompose.data.memory.MemoryEntryEntity

@Database(
    entities = [
        AgentMessageEntity::class,
        AgentContextEntity::class,
        AgentFactEntity::class,
        MemoryEntryEntity::class       // Day 11: таблица слоёв памяти
    ],
    version = 4,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentMessageDao(): AgentMessageDao
    abstract fun agentContextDao(): AgentContextDao
    abstract fun agentFactDao(): AgentFactDao
    abstract fun memoryDao(): MemoryDao  // Day 11
}
