 package com.example.hellocompose.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hellocompose.domain.model.QuizConfig
import com.example.hellocompose.domain.model.QuizDifficulty
import com.example.hellocompose.domain.model.QuizTopic
import kotlin.math.roundToInt
@Composable
private fun QuizSlider(
    title: String,
    description: String,
    displayValue: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = displayValue,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onStart: (QuizConfig) -> Unit
) {
    var selectedTopic by remember { mutableStateOf(QuizTopic.RANDOM) }
    var subtopic by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var questionCount by remember { mutableFloatStateOf(5f) }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var maxTokens by remember { mutableIntStateOf(500) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Викторина",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Тема
            Text(
                text = "Тема",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizTopic.entries.forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { selectedTopic = topic },
                        label = { Text(topic.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Подтема
            OutlinedTextField(
                value = subtopic,
                onValueChange = { subtopic = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = when (selectedTopic) {
                            QuizTopic.RUSSIAN -> "Например: н и нн в причастиях"
                            QuizTopic.SCIENCE -> "Например: законы термодинамики"
                            QuizTopic.HISTORY -> "Например: Вторая мировая война"
                            QuizTopic.GEOGRAPHY -> "Например: реки Азии"
                            QuizTopic.MOVIES -> "Например: фильмы Кубрика"
                            QuizTopic.TECHNOLOGY -> "Например: алгоритмы сортировки"
                            QuizTopic.SPORTS -> "Например: Олимпийские игры"
                            QuizTopic.RANDOM -> "Уточните тему (необязательно)..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                label = { Text("Подтема (необязательно)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Сложность
            Text(
                text = "Сложность",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizDifficulty.entries.forEach { difficulty ->
                    val isSelected = selectedDifficulty == difficulty
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        animationSpec = tween(200),
                        label = "diffBg"
                    )
                    Surface(
                        onClick = { selectedDifficulty = difficulty },
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = difficulty.displayName,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Количество вопросов
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Вопросов",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Количество вопросов в викторине",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${questionCount.roundToInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = questionCount,
                onValueChange = { questionCount = it },
                valueRange = 3f..15f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Temperature
            QuizSlider(
                title = "Temperature",
                description = "Креативность вопросов",
                displayValue = "%.2f".format(temperature),
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Max Tokens
            QuizSlider(
                title = "Max Tokens",
                description = "Длина одного ответа AI",
                displayValue = "$maxTokens",
                value = maxTokens.toFloat(),
                onValueChange = { maxTokens = it.roundToInt() },
                valueRange = 100f..2000f
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Кнопка старт
            Button(
                onClick = {
                    onStart(
                        QuizConfig(
                            topic = selectedTopic,
                            subtopic = subtopic.trim(),
                            difficulty = selectedDifficulty,
                            questionCount = questionCount.roundToInt(),
                            temperature = temperature,
                            maxTokens = maxTokens
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Начать викторину",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
