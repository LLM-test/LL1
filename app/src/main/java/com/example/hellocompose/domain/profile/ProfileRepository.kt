package com.example.hellocompose.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

    /** Реактивный поток профиля (null — профиль ещё не создан). */
    fun getProfile(): Flow<UserProfile?>

    /** Сохраняет (upsert) профиль. */
    suspend fun setProfile(profile: UserProfile)

    /** Удаляет профиль (сбрасывает к пустому). */
    suspend fun clearProfile()

    /**
     * Форматированный блок для system prompt.
     * Возвращает пустую строку, если профиль пустой.
     */
    suspend fun getProfilePrompt(): String
}
