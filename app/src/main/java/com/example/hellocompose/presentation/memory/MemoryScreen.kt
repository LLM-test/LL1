package com.example.hellocompose.presentation.memory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hellocompose.domain.memory.MemoryEntry
import com.example.hellocompose.domain.memory.MemoryType
import kotlinx.coroutines.flow.collectLatest

// ── Палитра ───────────────────────────────────────────────────────────────────

private val shortTermColor = Color(0xFF37474F) // blue-grey  (краткосрочная)
private val workingColor   = Color(0xFF1565C0) // blue       (рабочая)
private val longTermColor  = Color(0xFF2E7D32) // green      (долговременная)

private val tabLabels = listOf("💬 Краткосрочная", "🔧 Рабочая", "🧠 Долговременная")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDemo: () -> Unit = {}
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MemoryEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧠 Модель памяти", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToDemo) {
                        Text("🧪 Демо")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Вкладки
            TabRow(selectedTabIndex = state.selectedTab) {
                tabLabels.forEachIndexed { i, label ->
                    Tab(
                        selected = state.selectedTab == i,
                        onClick  = { viewModel.handleIntent(MemoryIntent.SelectTab(i)) },
                        text     = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            when (state.selectedTab) {
                0 -> ShortTermTab()
                1 -> MemoryLayerTab(
                    type      = MemoryType.WORKING,
                    entries   = state.workingMemory,
                    accentColor = workingColor,
                    newKey    = state.newKey,
                    newValue  = state.newValue,
                    onTypeKey = { viewModel.handleIntent(MemoryIntent.TypeKey(it)) },
                    onTypeValue = { viewModel.handleIntent(MemoryIntent.TypeValue(it)) },
                    onSave    = { viewModel.handleIntent(MemoryIntent.SaveEntry(MemoryType.WORKING)) },
                    onDelete  = { viewModel.handleIntent(MemoryIntent.DeleteEntry(it)) },
                    onClear   = { viewModel.handleIntent(MemoryIntent.ClearType(MemoryType.WORKING)) }
                )
                2 -> MemoryLayerTab(
                    type      = MemoryType.LONG_TERM,
                    entries   = state.longTermMemory,
                    accentColor = longTermColor,
                    newKey    = state.newKey,
                    newValue  = state.newValue,
                    onTypeKey = { viewModel.handleIntent(MemoryIntent.TypeKey(it)) },
                    onTypeValue = { viewModel.handleIntent(MemoryIntent.TypeValue(it)) },
                    onSave    = { viewModel.handleIntent(MemoryIntent.SaveEntry(MemoryType.LONG_TERM)) },
                    onDelete  = { viewModel.handleIntent(MemoryIntent.DeleteEntry(it)) },
                    onClear   = { viewModel.handleIntent(MemoryIntent.ClearType(MemoryType.LONG_TERM)) }
                )
            }
        }
    }
}

// ── Вкладка «Краткосрочная» (информационная) ─────────────────────────────────

@Composable
private fun ShortTermTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                icon  = "💬",
                title = "Краткосрочная память",
                color = shortTermColor,
                description = """
                    Это список сообщений текущего сеанса (RAM).

                    Хранится: в оперативной памяти.
                    Живёт: до закрытия приложения или нажатия «Очистить историю».
                    Передаётся в API: да — последние N сообщений (зависит от стратегии).
                """.trimIndent()
            )
        }
        item {
            InfoCard(
                icon  = "🔧",
                title = "Рабочая память",
                color = workingColor,
                description = """
                    Текущая задача, стек, цель.

                    Хранится: Room (таблица memory_entries).
                    Живёт: до ручной очистки.
                    Передаётся в API: да — как блок [РАБОЧАЯ ПАМЯТЬ] в system prompt.
                """.trimIndent()
            )
        }
        item {
            InfoCard(
                icon  = "🧠",
                title = "Долговременная память",
                color = longTermColor,
                description = """
                    Профиль пользователя, ключевые решения, знания.

                    Хранится: Room (таблица memory_entries).
                    Живёт: всегда — пока не удалите вручную.
                    Передаётся в API: да — как блок [ДОЛГОВРЕМЕННАЯ ПАМЯТЬ] в system prompt.

                    Агент «помнит» имя и контекст даже после сброса истории!
                """.trimIndent()
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("📋 Формат в system prompt", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = """
=== ПАМЯТЬ АССИСТЕНТА ===
[ДОЛГОВРЕМЕННАЯ ПАМЯТЬ]
profile.name: Андрей
profile.expertise: Android Kotlin
[РАБОЧАЯ ПАМЯТЬ]
task.name: Трекер привычек
task.goal: Реализовать напоминания
                        """.trimIndent(),
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 11.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(icon: String, title: String, color: Color, description: String) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = color.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = color,
                    style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                lineHeight = 18.sp)
        }
    }
}

// ── Вкладка слоя памяти (Рабочая / Долговременная) ───────────────────────────

@Composable
private fun MemoryLayerTab(
    type: MemoryType,
    entries: List<MemoryEntry>,
    accentColor: Color,
    newKey: String,
    newValue: String,
    onTypeKey: (String) -> Unit,
    onTypeValue: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить «${type.displayName}»?") },
            text  = { Text("Все записи будут удалены. Агент перестанет их учитывать.") },
            confirmButton = {
                TextButton(onClick = { onClear(); showClearDialog = false }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Отмена") }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Заголовок + кнопка очистки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${type.icon} ${type.displayName}",
                fontWeight = FontWeight.Bold,
                color = accentColor,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Очистить всё",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }

        Divider()

        // Список записей
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 10.dp
            )
        ) {
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Память пуста",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    MemoryEntryRow(entry = entry, accentColor = accentColor, onDelete = onDelete)
                }
            }
        }

        Divider()

        // Форма добавления
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Добавить в «${type.displayName}»",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
            OutlinedTextField(
                value = newKey,
                onValueChange = onTypeKey,
                label = { Text("Ключ  (напр. profile.name, task.goal)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newValue,
                onValueChange = onTypeValue,
                label = { Text("Значение") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                onClick = onSave,
                shape   = RoundedCornerShape(50),
                color   = accentColor,
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(18.dp))
                    Text("Сохранить", color = Color.White, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Строка одной записи памяти ────────────────────────────────────────────────

@Composable
private fun MemoryEntryRow(entry: MemoryEntry, accentColor: Color, onDelete: (Long) -> Unit) {
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = accentColor.copy(alpha = 0.07f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ключ-точка
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = entry.key,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = entry.value,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
            IconButton(
                onClick = { onDelete(entry.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
