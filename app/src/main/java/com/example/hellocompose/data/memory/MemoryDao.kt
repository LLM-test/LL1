package com.example.hellocompose.data.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memory_entries ORDER BY createdAt ASC")
    fun getAll(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE type = :type ORDER BY createdAt ASC")
    fun getByType(type: String): Flow<List<MemoryEntryEntity>>

    /** Используется синхронно при построении system prompt внутри корутины. */
    @Query("SELECT * FROM memory_entries ORDER BY type DESC, createdAt ASC")
    suspend fun getAllSync(): List<MemoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntryEntity): Long

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_entries WHERE type = :type")
    suspend fun clearByType(type: String)
}
