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

class DeepSeekApiService(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatCompletions(request: ChatRequestDto): ChatResponseDto {
        val response: HttpResponse = client.post("${ApiConstants.BASE_URL}chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${ApiConstants.API_KEY}")
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

class ApiException(val statusCode: Int, message: String) : Exception(message)
