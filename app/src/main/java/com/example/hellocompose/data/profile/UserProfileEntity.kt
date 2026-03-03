package com.example.hellocompose.data.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hellocompose.domain.profile.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "",
    val expertise: String = "",
    val responseStyle: String = "",
    val responseFormat: String = "",
    val preferredLanguage: String = "",
    val constraints: String = ""
) {
    fun toDomain() = UserProfile(
        id                = id,
        name              = name,
        expertise         = expertise,
        responseStyle     = responseStyle,
        responseFormat    = responseFormat,
        preferredLanguage = preferredLanguage,
        constraints       = constraints
    )
}

fun UserProfile.toEntity() = UserProfileEntity(
    id                = id,
    name              = name,
    expertise         = expertise,
    responseStyle     = responseStyle,
    responseFormat    = responseFormat,
    preferredLanguage = preferredLanguage,
    constraints       = constraints
)
