package com.example.hellocompose.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hellocompose.data.api.dto.MessageDto
import com.example.hellocompose.data.api.dto.ToolCallDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "agent_messages")
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String? = null,
    @ColumnInfo(name = "tool_calls_json") val toolCallsJson: String? = null,
    @ColumnInfo(name = "tool_call_id") val toolCallId: String? = null
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

fun AgentMessageEntity.toMessageDto(): MessageDto {
    val toolCalls = toolCallsJson?.let { json.decodeFromString<List<ToolCallDto>>(it) }
    return MessageDto(
        role = role,
        content = content,
        toolCalls = toolCalls,
        toolCallId = toolCallId
    )
}

fun MessageDto.toEntity(): AgentMessageEntity = AgentMessageEntity(
    role = role,
    content = content,
    toolCallsJson = toolCalls?.let { json.encodeToString(it) },
    toolCallId = toolCallId
)
