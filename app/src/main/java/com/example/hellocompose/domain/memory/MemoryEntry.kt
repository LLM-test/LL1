package com.example.hellocompose.domain.memory

/** Единица памяти: ключ-значение с указанием слоя. */
data class MemoryEntry(
    val id: Long = 0,
    val type: MemoryType,
    val key: String,
    val value: String,
    val createdAt: Long = System.currentTimeMillis()
)
