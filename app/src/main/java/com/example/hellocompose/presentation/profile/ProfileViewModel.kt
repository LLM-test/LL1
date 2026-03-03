package com.example.hellocompose.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hellocompose.domain.profile.ProfilePresets
import com.example.hellocompose.domain.profile.ProfileRepository
import com.example.hellocompose.domain.profile.UserProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repo: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Подписываемся на профиль из БД — заполняем draft при первой загрузке
        viewModelScope.launch {
            repo.getProfile().collect { profile ->
                if (profile != null) {
                    _state.update {
                        it.copy(
                            name             = profile.name,
                            expertise        = profile.expertise,
                            responseStyle    = profile.responseStyle,
                            responseFormat   = profile.responseFormat,
                            preferredLanguage = profile.preferredLanguage,
                            constraints      = profile.constraints
                        )
                    }
                }
                refreshPreview()
            }
        }
    }

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.UpdateName             -> _state.update { it.copy(name = intent.value) }
            is ProfileIntent.UpdateExpertise        -> _state.update { it.copy(expertise = intent.value) }
            is ProfileIntent.UpdateResponseStyle    -> _state.update { it.copy(responseStyle = intent.value) }
            is ProfileIntent.UpdateResponseFormat   -> _state.update { it.copy(responseFormat = intent.value) }
            is ProfileIntent.UpdatePreferredLanguage -> _state.update { it.copy(preferredLanguage = intent.value) }
            is ProfileIntent.UpdateConstraints      -> _state.update { it.copy(constraints = intent.value) }
            is ProfileIntent.ApplyPreset            -> applyPreset(intent.preset)
            is ProfileIntent.Save                   -> save()
            is ProfileIntent.Clear                  -> clear()
        }
    }

    private fun applyPreset(preset: ProfilePresetType) {
        val p = when (preset) {
            ProfilePresetType.JUNIOR  -> ProfilePresets.JUNIOR
            ProfilePresetType.SENIOR  -> ProfilePresets.SENIOR
            ProfilePresetType.STUDENT -> ProfilePresets.STUDENT
        }
        _state.update {
            it.copy(
                name              = p.name,
                expertise         = p.expertise,
                responseStyle     = p.responseStyle,
                responseFormat    = p.responseFormat,
                preferredLanguage = p.preferredLanguage,
                constraints       = p.constraints
            )
        }
    }

    private fun save() {
        viewModelScope.launch {
            val s = _state.value
            repo.setProfile(UserProfile(
                name              = s.name,
                expertise         = s.expertise,
                responseStyle     = s.responseStyle,
                responseFormat    = s.responseFormat,
                preferredLanguage = s.preferredLanguage,
                constraints       = s.constraints
            ))
            refreshPreview()
            _effect.send(ProfileEffect.ProfileSaved)
        }
    }

    private fun clear() {
        viewModelScope.launch {
            repo.clearProfile()
            _state.update {
                it.copy(
                    name = "", expertise = "", responseStyle = "",
                    responseFormat = "", preferredLanguage = "", constraints = ""
                )
            }
            refreshPreview()
            _effect.send(ProfileEffect.ProfileCleared)
        }
    }

    private suspend fun refreshPreview() {
        val preview = repo.getProfilePrompt()
        _state.update { it.copy(systemPromptPreview = preview) }
    }
}
