package com.example.hellocompose.presentation.profile

// ── State ─────────────────────────────────────────────────────────────────────

data class ProfileState(
    val name: String = "",
    val expertise: String = "",
    val responseStyle: String = "",
    val responseFormat: String = "",
    val preferredLanguage: String = "",
    val constraints: String = "",
    val isSaved: Boolean = false,           // флаг для показа Toast
    val systemPromptPreview: String = ""    // live preview блока профиля
)

// ── Intent ────────────────────────────────────────────────────────────────────

sealed interface ProfileIntent {
    data class UpdateName(val value: String)             : ProfileIntent
    data class UpdateExpertise(val value: String)        : ProfileIntent
    data class UpdateResponseStyle(val value: String)    : ProfileIntent
    data class UpdateResponseFormat(val value: String)   : ProfileIntent
    data class UpdatePreferredLanguage(val value: String): ProfileIntent
    data class UpdateConstraints(val value: String)      : ProfileIntent
    data class ApplyPreset(val preset: ProfilePresetType): ProfileIntent
    object Save                                          : ProfileIntent
    object Clear                                         : ProfileIntent
}

enum class ProfilePresetType { JUNIOR, SENIOR, STUDENT }

// ── Effect ────────────────────────────────────────────────────────────────────

sealed interface ProfileEffect {
    object ProfileSaved   : ProfileEffect
    object ProfileCleared : ProfileEffect
}
