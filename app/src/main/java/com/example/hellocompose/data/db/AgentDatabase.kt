package com.example.hellocompose.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hellocompose.data.memory.MemoryDao
import com.example.hellocompose.data.memory.MemoryEntryEntity
import com.example.hellocompose.data.profile.ProfileDao
import com.example.hellocompose.data.profile.UserProfileEntity

@Database(
    entities = [
        AgentMessageEntity::class,
        AgentContextEntity::class,
        AgentFactEntity::class,
        MemoryEntryEntity::class,      // Day 11: таблица слоёв памяти
        UserProfileEntity::class       // Day 12: профиль пользователя (единственная запись)
    ],
    version = 5,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentMessageDao(): AgentMessageDao
    abstract fun agentContextDao(): AgentContextDao
    abstract fun agentFactDao(): AgentFactDao
    abstract fun memoryDao(): MemoryDao       // Day 11
    abstract fun profileDao(): ProfileDao     // Day 12
}
