package com.example.hellocompose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AgentContextDao {

    @Query("SELECT * FROM agent_context WHERE id = 1")
    suspend fun get(): AgentContextEntity?

    /** Upsert — вставляет или заменяет единственную строку. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: AgentContextEntity)

    @Query("DELETE FROM agent_context")
    suspend fun clear()
}
