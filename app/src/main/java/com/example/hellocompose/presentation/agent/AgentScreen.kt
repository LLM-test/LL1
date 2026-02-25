package com.example.hellocompose.presentation.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hellocompose.domain.agent.AgentStep
import com.example.hellocompose.domain.agent.TokenInfo
import com.example.hellocompose.presentation.components.ChatInput
import kotlinx.coroutines.flow.collectLatest

private val agentColor = Color(0xFF00695C) // teal
private val warnColor = Color(0xFFF57F17)  // amber
private val dangerColor = Color(0xFFB71C1C) // dark red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AgentEffect.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.lastIndex)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 Агент") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = agentColor,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.handleIntent(AgentIntent.ClearHistory)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Очистить",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Строка с доступными инструментами
            ToolsInfoRow()

            // Панель статистики токенов (появляется после первого ответа)
            SessionStatsBar(stats = state.sessionStats)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item { AgentEmptyState(modifier = Modifier.fillParentMaxSize()) }
                }

                items(items = state.messages, key = { it.id }) { message ->
                    when (message) {
                        is AgentMessage.User -> UserBubble(message.text)
                        is AgentMessage.Assistant -> AssistantCard(message)
                    }
                }
            }

            ChatInput(
                inputText = state.inputText,
                isLoading = state.isLoading,
                onTextChange = { viewModel.handleIntent(AgentIntent.TypeMessage(it)) },
                onSendClick = { viewModel.handleIntent(AgentIntent.SendMessage) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Панель статистики токенов сессии
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionStatsBar(stats: SessionStats) {
    if (stats.totalExchanges == 0) return

    val progress = stats.contextUsedPercent
    val barColor = when {
        progress > 0.9f -> dangerColor
        progress > 0.8f -> warnColor
        else -> agentColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Контекст — текущий запрос
            Text(
                text = "Контекст: ${formatTokens(stats.lastPromptTokens)} / 128K",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
            // Суммарная стоимость сессии
            Text(
                text = "Итого: $${String.format("%.4f", stats.totalCostUsd)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Прогресс-бар заполнения контекста
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )

        // Предупреждение при >80% заполнения
        if (stats.isNearLimit) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "⚠️ Контекст заполнен на ${(progress * 100).toInt()}%. " +
                    "При переполнении агент вернёт ошибку.",
                style = MaterialTheme.typography.labelSmall,
                color = warnColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Строка с доступными инструментами
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolsInfoRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(agentColor.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Инструменты:",
            style = MaterialTheme.typography.labelSmall,
            color = agentColor,
            fontWeight = FontWeight.SemiBold
        )
        ToolChip("🕐 Дата и время")
        ToolChip("🧮 Калькулятор")
    }
}

@Composable
private fun ToolChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = agentColor.copy(alpha = 0.12f),
        modifier = Modifier.border(1.dp, agentColor.copy(alpha = 0.3f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = agentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Пустое состояние
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AgentEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🤖", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Агент готов к работе",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Агент сам решает, когда\nиспользовать инструменты.\nСпроси про время или вычисление.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Пузырь пользователя
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                .background(agentColor)
                .padding(12.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Карточка ответа агента
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AssistantCard(message: AgentMessage.Assistant, modifier: Modifier = Modifier) {
    val borderColor = if (message.isError)
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    else
        agentColor.copy(alpha = 0.3f)
    val bgColor = if (message.isError)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    else
        agentColor.copy(alpha = 0.07f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Шапка агента
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(agentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🤖", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "Агент",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = agentColor
                    )
                    Text(
                        text = "deepseek-chat + инструменты",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            if (message.isLoading) {
                Spacer(Modifier.height(8.dp))
                AgentLoadingDots()
            } else {
                // Шаги (вызовы инструментов)
                if (message.steps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    message.steps.forEach { step ->
                        ToolStepCard(step = step)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // Финальный ответ
                if (message.text.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isError)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                // Статистика токенов (только для новых сообщений с tokenInfo)
                message.tokenInfo?.let { info ->
                    if (info.totalTokens > 0) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = agentColor.copy(alpha = 0.15f))
                        Spacer(Modifier.height(6.dp))
                        TokenInfoRow(info)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Строка статистики токенов внутри карточки
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TokenInfoRow(info: TokenInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Входные токены (запрос + история)
        TokenBadge(
            emoji = "📥",
            label = "запрос",
            value = formatTokens(info.promptTokens),
            color = Color(0xFF1565C0) // синий
        )
        // Выходные токены (ответ)
        TokenBadge(
            emoji = "📤",
            label = "ответ",
            value = formatTokens(info.completionTokens),
            color = agentColor
        )
        Spacer(Modifier.weight(1f))
        // Стоимость
        Text(
            text = "💵 $${String.format("%.5f", info.costUsd)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TokenBadge(emoji: String, label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = emoji, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Карточка вызова инструмента — раскрываемая
// ─────────────────────────────────────────────────────────────────────────────

private val toolColor = Color(0xFF6A1B9A) // фиолетовый

@Composable
private fun ToolStepCard(step: AgentStep) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = toolColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, toolColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
    ) {
        Column {
            // Заголовок — всегда виден, кликабелен
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = toolColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = step.toolName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = toolColor
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = toolColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Детали — раскрываются по клику
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                ) {
                    if (step.arguments != "{}") {
                        Text(
                            text = "Аргументы: ${step.arguments}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = toolColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = step.result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Анимированные точки загрузки
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AgentLoadingDots() {
    val transition = rememberInfiniteTransition(label = "agent_loading")
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0f at (index * 200) using LinearEasing
                        -4f at (index * 200 + 200) using LinearEasing
                        0f at (index * 200 + 400) using LinearEasing
                    }
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(agentColor.copy(alpha = 0.6f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Хелперы
// ─────────────────────────────────────────────────────────────────────────────

/** Форматирует количество токенов: 1500 → "1.5K", 500 → "500". */
private fun formatTokens(n: Int): String = when {
    n >= 10_000 -> "${n / 1000}K"
    n >= 1_000 -> "${n / 1000}.${(n % 1000) / 100}K"
    else -> "$n"
}
