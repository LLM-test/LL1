package com.example.hellocompose.domain.agent.tools

import com.example.hellocompose.data.api.dto.FunctionDefinitionDto
import com.example.hellocompose.data.api.dto.ParametersDto
import com.example.hellocompose.data.api.dto.PropertyDto
import com.example.hellocompose.data.api.dto.ToolDefinitionDto
import com.example.hellocompose.domain.agent.AgentTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Инструмент: вычисляет математическое выражение (+, -, *, /, скобки). */
class CalculatorTool : AgentTool {

    override val name = "calculate"

    override val definition = ToolDefinitionDto(
        function = FunctionDefinitionDto(
            name = name,
            description = "Вычисляет математическое выражение. Поддерживает +, -, *, /, скобки.",
            parameters = ParametersDto(
                properties = mapOf(
                    "expression" to PropertyDto(
                        type = "string",
                        description = "Математическое выражение, например «12 * (3 + 4)»"
                    )
                ),
                required = listOf("expression")
            )
        )
    )

    override suspend fun execute(argumentsJson: String): String {
        return try {
            val json = Json.parseToJsonElement(argumentsJson).jsonObject
            val expr = json["expression"]?.jsonPrimitive?.content
                ?: return "Ошибка: параметр expression не указан"
            val result = MathEvaluator(expr).evaluate()
            val formatted = if (result == kotlin.math.floor(result) && !result.isInfinite()) {
                result.toLong().toString()
            } else {
                "%.6f".format(result).trimEnd('0').trimEnd('.')
            }
            "Результат: $formatted"
        } catch (e: Exception) {
            "Ошибка вычисления: ${e.message}"
        }
    }
}

/** Простой рекурсивный парсер арифметических выражений. */
private class MathEvaluator(expression: String) {
    private val input = expression.replace(" ", "")
    private var pos = 0

    fun evaluate(): Double = parseExpr()

    private fun parseExpr(): Double {
        var result = parseTerm()
        while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
            val op = input[pos++]
            val term = parseTerm()
            result = if (op == '+') result + term else result - term
        }
        return result
    }

    private fun parseTerm(): Double {
        var result = parseFactor()
        while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
            val op = input[pos++]
            val factor = parseFactor()
            result = if (op == '*') result * factor else result / factor
        }
        return result
    }

    private fun parseFactor(): Double {
        if (pos < input.length && input[pos] == '(') {
            pos++
            val result = parseExpr()
            if (pos < input.length && input[pos] == ')') pos++
            return result
        }
        if (pos < input.length && input[pos] == '-') {
            pos++
            return -parseFactor()
        }
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        return input.substring(start, pos).toDouble()
    }
}
