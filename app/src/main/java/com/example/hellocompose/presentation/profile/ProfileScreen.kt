package com.example.hellocompose.presentation.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToDemo: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Обрабатываем эффекты (Toast)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ProfileSaved   -> Toast.makeText(context, "Профиль сохранён", Toast.LENGTH_SHORT).show()
                is ProfileEffect.ProfileCleared -> Toast.makeText(context, "Профиль очищен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Профиль пользователя") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToDemo) { Text("🧪 Демо") }
                    IconButton(onClick = { viewModel.handleIntent(ProfileIntent.Clear) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Очистить профиль")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Пресеты ───────────────────────────────────────────────────────
            Text(
                text = "Быстрые пресеты",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PresetChip(
                    label = "👶 Junior",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.handleIntent(ProfileIntent.ApplyPreset(ProfilePresetType.JUNIOR)) }
                )
                PresetChip(
                    label = "👨‍💻 Senior",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.handleIntent(ProfileIntent.ApplyPreset(ProfilePresetType.SENIOR)) }
                )
                PresetChip(
                    label = "🎓 Студент",
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.handleIntent(ProfileIntent.ApplyPreset(ProfilePresetType.STUDENT)) }
                )
            }
            Text(
                text = "Пресет заполняет поля, но не сохраняет автоматически",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Форма ────────────────────────────────────────────────────────
            Text(
                text = "Данные профиля",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdateName(it)) },
                label = { Text("Имя") },
                placeholder = { Text("Как вас зовут?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.expertise,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdateExpertise(it)) },
                label = { Text("Уровень / Роль") },
                placeholder = { Text("Например: Junior Developer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.responseStyle,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdateResponseStyle(it)) },
                label = { Text("Стиль ответов") },
                placeholder = { Text("Например: подробный и понятный") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.responseFormat,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdateResponseFormat(it)) },
                label = { Text("Формат ответов") },
                placeholder = { Text("Например: с примерами кода") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.preferredLanguage,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdatePreferredLanguage(it)) },
                label = { Text("Язык ответов") },
                placeholder = { Text("Например: Русский") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.constraints,
                onValueChange = { viewModel.handleIntent(ProfileIntent.UpdateConstraints(it)) },
                label = { Text("Ограничения / Пожелания") },
                placeholder = { Text("Например: не объясняй базовые концепции") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // ── Кнопка сохранить ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.handleIntent(ProfileIntent.Save) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Сохранить", fontWeight = FontWeight.SemiBold)
            }

            // ── System Prompt Preview ─────────────────────────────────────────
            SystemPromptPreview(preview = state.systemPromptPreview)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Пресет-чип ────────────────────────────────────────────────────────────────

@Composable
private fun PresetChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── System Prompt Preview ─────────────────────────────────────────────────────

@Composable
private fun SystemPromptPreview(preview: String) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 System prompt preview",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                if (preview.isBlank()) {
                    Text(
                        text = "Профиль пустой — блок не добавляется в system prompt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = preview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
