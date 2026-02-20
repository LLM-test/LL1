package com.example.hellocompose.data.api

import com.example.hellocompose.data.api.dto.ApiErrorDto
import com.example.hellocompose.data.api.dto.ChatRequestDto
import com.example.hellocompose.data.api.dto.ChatResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Универсальный OpenAI-совместимый сервис.
 * [baseUrl] должен оканчиваться на "/", например "https://api.groq.com/openai/v1/"
 */
class ModelComparisonApiService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val apiKey: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatCompletions(request: ChatRequestDto): ChatResponseDto {
        val response: HttpResponse = client.post("${baseUrl}chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorMessage = try {
                val errorBody = response.bodyAsText()
                val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
                apiError.error.message
            } catch (_: Exception) {
                "API error: ${response.status.value} ${response.status.description}"
            }
            throw ApiException(response.status.value, errorMessage)
        }

        return response.body()
    }
}
