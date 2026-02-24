package com.example.hellocompose.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToolDefinitionDto(
    @SerialName("type")
    val type: String = "function",
    @SerialName("function")
    val function: FunctionDefinitionDto
)

@Serializable
data class FunctionDefinitionDto(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("parameters")
    val parameters: ParametersDto
)

@Serializable
data class ParametersDto(
    @SerialName("type")
    val type: String = "object",
    @SerialName("properties")
    val properties: Map<String, PropertyDto> = emptyMap(),
    @SerialName("required")
    val required: List<String> = emptyList()
)

@Serializable
data class PropertyDto(
    @SerialName("type")
    val type: String,
    @SerialName("description")
    val description: String
)

@Serializable
data class ToolCallDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String = "function",
    @SerialName("function")
    val function: ToolCallFunctionDto
)

@Serializable
data class ToolCallFunctionDto(
    @SerialName("name")
    val name: String,
    @SerialName("arguments")
    val arguments: String
)
