package com.example.hellocompose.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hellocompose.data.api.dto.QuizResponseDto

@Composable
fun QuizMessageBubble(
    quizData: QuizResponseDto,
    onOptionSelected: ((optionKey: String, optionText: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (quizData) {
        is QuizResponseDto.Question -> QuizQuestionCard(quizData, onOptionSelected, modifier)
        is QuizResponseDto.Answer -> QuizAnswerCard(quizData, modifier)
        is QuizResponseDto.Final -> QuizFinalCard(quizData, modifier)
    }
}

@Composable
private fun QuizQuestionCard(
    data: QuizResponseDto.Question,
    onOptionSelected: ((String, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var selectedKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // Счётчик вопроса
        Text(
            text = "📌 Вопрос ${data.questionNumber}/${data.total}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Текст вопроса
        Text(
            text = data.question,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Варианты ответа
        data.options.toList().forEach { (key, value) ->
            val isSelected = selectedKey == key
            val containerColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface,
                animationSpec = tween(200),
                label = "opt_bg_$key"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                animationSpec = tween(200),
                label = "opt_border_$key"
            )

            Surface(
                onClick = {
                    if (selectedKey == null && onOptionSelected != null) {
                        selectedKey = key
                        onOptionSelected(key, value)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                enabled = selectedKey == null && onOptionSelected != null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кружок с буквой
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = key,
                            modifier = Modifier.padding(4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizAnswerCard(
    data: QuizResponseDto.Answer,
    modifier: Modifier = Modifier
) {
    val bgColor = if (data.correct)
        Color(0xFF1B5E20).copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    val accentColor = if (data.correct)
        Color(0xFF2E7D32)
    else
        MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (data.correct) "✅ Правильно!" else "❌ Неверно",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Счёт: ${data.score}/${data.total}",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        if (!data.correct) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 Правильный ответ: ${data.correctOption} — ${data.correctText}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        if (data.explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun QuizFinalCard(
    data: QuizResponseDto.Final,
    modifier: Modifier = Modifier
) {
    val ratio = if (data.total > 0) data.score.toFloat() / data.total else 0f
    val bgColor = when {
        ratio >= 0.8f -> Color(0xFF1B5E20).copy(alpha = 0.12f)
        ratio >= 0.5f -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    }
    val emoji = when {
        ratio >= 0.8f -> "🏆"
        ratio >= 0.5f -> "👍"
        else -> "📚"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$emoji Викторина завершена!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${data.score} / ${data.total}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = data.comment,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
