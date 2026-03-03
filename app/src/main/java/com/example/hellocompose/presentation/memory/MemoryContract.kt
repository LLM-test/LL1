package com.example.hellocompose.presentation.memory

import com.example.hellocompose.domain.memory.MemoryEntry
import com.example.hellocompose.domain.memory.MemoryType

// ── State ─────────────────────────────────────────────────────────────────────

data class MemoryState(
    val workingMemory: List<MemoryEntry> = emptyList(),
    val longTermMemory: List<MemoryEntry> = emptyList(),
    val selectedTab: Int = 0,           // 0=краткосрочная (info), 1=рабочая, 2=долгосрочная
    val newKey: String = "",
    val newValue: String = ""
)

// ── Intents ───────────────────────────────────────────────────────────────────

sealed class MemoryIntent {
    data class SelectTab(val index: Int) : MemoryIntent()
    data class TypeKey(val text: String) : MemoryIntent()
    data class TypeValue(val text: String) : MemoryIntent()
    data class SaveEntry(val type: MemoryType) : MemoryIntent()
    data class DeleteEntry(val id: Long) : MemoryIntent()
    data class ClearType(val type: MemoryType) : MemoryIntent()
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed class MemoryEffect {
    data class ShowToast(val message: String) : MemoryEffect()
}
