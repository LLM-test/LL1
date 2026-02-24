package com.example.hellocompose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AgentMessageDao {

    @Query("SELECT * FROM agent_messages ORDER BY id ASC")
    suspend fun getAll(): List<AgentMessageEntity>

    @Insert
    suspend fun insert(message: AgentMessageEntity)

    @Query("DELETE FROM agent_messages")
    suspend fun deleteAll()
}
