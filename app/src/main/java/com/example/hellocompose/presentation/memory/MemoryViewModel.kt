package com.example.hellocompose.presentation.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.memory.MemoryRepository
import com.example.hellocompose.domain.memory.MemoryType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryViewModel(private val repo: MemoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(MemoryState())
    val state: StateFlow<MemoryState> = _state.asStateFlow()

    private val _effect = Channel<MemoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.getByType(MemoryType.WORKING).collect { list ->
                _state.update { it.copy(workingMemory = list) }
            }
        }
        viewModelScope.launch {
            repo.getByType(MemoryType.LONG_TERM).collect { list ->
                _state.update { it.copy(longTermMemory = list) }
            }
        }
    }

    fun handleIntent(intent: MemoryIntent) {
        when (intent) {
            is MemoryIntent.SelectTab    -> _state.update { it.copy(selectedTab = intent.index) }
            is MemoryIntent.TypeKey      -> _state.update { it.copy(newKey = intent.text) }
            is MemoryIntent.TypeValue    -> _state.update { it.copy(newValue = intent.text) }
            is MemoryIntent.SaveEntry    -> saveEntry(intent.type)
            is MemoryIntent.DeleteEntry  -> deleteEntry(intent.id)
            is MemoryIntent.ClearType    -> clearType(intent.type)
        }
    }

    private fun saveEntry(type: MemoryType) {
        val key   = _state.value.newKey.trim()
        val value = _state.value.newValue.trim()
        if (key.isBlank() || value.isBlank()) {
            viewModelScope.launch { _effect.send(MemoryEffect.ShowToast("Заполните ключ и значение")) }
            return
        }
        viewModelScope.launch {
            repo.save(type, key, value)
            _state.update { it.copy(newKey = "", newValue = "") }
            _effect.send(MemoryEffect.ShowToast("${type.icon} Сохранено в «${type.displayName}»"))
        }
    }

    private fun deleteEntry(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    private fun clearType(type: MemoryType) {
        viewModelScope.launch {
            repo.clearByType(type)
            _effect.send(MemoryEffect.ShowToast("${type.icon} «${type.displayName}» очищена"))
        }
    }
}
