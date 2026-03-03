package com.example.hellocompose.data.profile

import com.example.hellocompose.domain.profile.ProfileRepository
import com.example.hellocompose.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: ProfileDao
) : ProfileRepository {

    override fun getProfile(): Flow<UserProfile?> =
        dao.getProfile().map { it?.toDomain() }

    override suspend fun setProfile(profile: UserProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun clearProfile() {
        dao.deleteAll()
    }

    /**
     * Форматирует блок === ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ === для system prompt.
     * Включает только непустые поля. Возвращает "" если профиль не заполнен.
     */
    override suspend fun getProfilePrompt(): String {
        val p = dao.getProfileSync()?.toDomain() ?: return ""
        if (p.isEmpty) return ""

        return buildString {
            appendLine("=== ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ ===")
            if (p.name.isNotBlank())              appendLine("Имя: ${p.name}")
            if (p.expertise.isNotBlank())         appendLine("Уровень: ${p.expertise}")
            if (p.responseStyle.isNotBlank())     appendLine("Стиль ответов: ${p.responseStyle}")
            if (p.responseFormat.isNotBlank())    appendLine("Формат ответов: ${p.responseFormat}")
            if (p.preferredLanguage.isNotBlank()) appendLine("Язык: ${p.preferredLanguage}")
            if (p.constraints.isNotBlank())       appendLine("Ограничения: ${p.constraints}")
        }.trimEnd()
    }
}
