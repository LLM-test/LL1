package com.example.hellocompose.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.hellocompose.domain.model.ChatSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: ChatSettings,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApply: (ChatSettings) -> Unit
) {
    var systemPrompt by remember(settings) { mutableStateOf(settings.systemPrompt) }
    var temperature by remember(settings) { mutableFloatStateOf(settings.temperature) }
    var maxTokens by remember(settings) { mutableIntStateOf(settings.maxTokens) }
    var topP by remember(settings) { mutableFloatStateOf(settings.topP) }
    var frequencyPenalty by remember(settings) { mutableFloatStateOf(settings.frequencyPenalty) }
    var presencePenalty by remember(settings) { mutableFloatStateOf(settings.presencePenalty) }

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
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    TextButton(onClick = {
                        val defaults = ChatSettings()
                        systemPrompt = defaults.systemPrompt
                        temperature = defaults.temperature
                        maxTokens = defaults.maxTokens
                        topP = defaults.topP
                        frequencyPenalty = defaults.frequencyPenalty
                        presencePenalty = defaults.presencePenalty
                    }) {
                        Text("Сброс")
                    }
                    TextButton(onClick = {
                        onApply(
                            ChatSettings(
                                systemPrompt = systemPrompt,
                                temperature = temperature,
                                maxTokens = maxTokens,
                                topP = topP,
                                frequencyPenalty = frequencyPenalty,
                                presencePenalty = presencePenalty
                            )
                        )
                        onDismiss()
                    }) {
                        Text("Применить")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Системный промт
            Text(
                text = "Системный промт",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Задайте роль или контекст для AI...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Temperature
            SettingsSlider(
                title = "Temperature",
                description = "Креативность ответов",
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f,
                displayValue = "%.2f".format(temperature)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Max Tokens
            SettingsSlider(
                title = "Max Tokens",
                description = "Максимальная длина ответа",
                value = maxTokens.toFloat(),
                onValueChange = { maxTokens = it.roundToInt() },
                valueRange = 256f..8192f,
                steps = 31,
                displayValue = "$maxTokens"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top P
            SettingsSlider(
                title = "Top P",
                description = "Ядерная выборка",
                value = topP,
                onValueChange = { topP = it },
                valueRange = 0f..1f,
                displayValue = "%.2f".format(topP)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Penalty
            SettingsSlider(
                title = "Frequency Penalty",
                description = "Штраф за частоту повторений",
                value = frequencyPenalty,
                onValueChange = { frequencyPenalty = it },
                valueRange = -2f..2f,
                displayValue = "%.2f".format(frequencyPenalty)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Presence Penalty
            SettingsSlider(
                title = "Presence Penalty",
                description = "Штраф за присутствие тем",
                value = presencePenalty,
                onValueChange = { presencePenalty = it },
                valueRange = -2f..2f,
                displayValue = "%.2f".format(presencePenalty)
            )
        }
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: String
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
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
