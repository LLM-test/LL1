package com.example.hellocompose.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AgentFactDao {

    @Query("SELECT * FROM agent_facts ORDER BY key ASC")
    suspend fun getAll(): List<AgentFactEntity>

    /** Upsert: вставляет или заменяет факт с данным ключом. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fact: AgentFactEntity)

    @Query("DELETE FROM agent_facts")
    suspend fun clear()
}
