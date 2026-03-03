package com.example.hellocompose.data.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,       // MemoryType.name (WORKING | LONG_TERM)
    val key: String,
    val value: String,
    val createdAt: Long = System.currentTimeMillis()
)
