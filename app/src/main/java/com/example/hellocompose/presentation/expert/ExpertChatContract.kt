package com.example.hellocompose.presentation.expert

import com.example.hellocompose.domain.model.ExpertCharacter
import com.example.hellocompose.domain.model.ExpertMessage

data class ExpertChatState(
    val selectedCharacters: List<ExpertCharacter> = emptyList(),
    val messages: List<ExpertMessage> = emptyList(),
    val inputText: String = "",
    val isAnyLoading: Boolean = false,
    val showCharacterPicker: Boolean = true
)

sealed interface ExpertChatIntent {
    data class TypeMessage(val text: String) : ExpertChatIntent
    data object SendMessage : ExpertChatIntent
    data class ToggleCharacter(val character: ExpertCharacter) : ExpertChatIntent
    data object ConfirmCharacters : ExpertChatIntent
    data object ResetCharacters : ExpertChatIntent
}

sealed interface ExpertChatEffect {
    data object ScrollToBottom : ExpertChatEffect
}
