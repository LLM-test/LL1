package com.example.hellocompose.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AgentMessageEntity::class,
        AgentContextEntity::class,
        AgentFactEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentMessageDao(): AgentMessageDao
    abstract fun agentContextDao(): AgentContextDao
    abstract fun agentFactDao(): AgentFactDao
}
