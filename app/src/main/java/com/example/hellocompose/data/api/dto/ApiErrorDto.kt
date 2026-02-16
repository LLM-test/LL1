package com.example.hellocompose.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    @SerialName("error")
    val error: ErrorDetailDto
)

@Serializable
data class ErrorDetailDto(
    @SerialName("message")
    val message: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("code")
    val code: String? = null
)
