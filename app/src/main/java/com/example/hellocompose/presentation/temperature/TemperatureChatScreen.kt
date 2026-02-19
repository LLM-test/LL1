package com.example.hellocompose.presentation.temperature

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hellocompose.domain.model.TemperatureResponse
import com.example.hellocompose.domain.model.TemperatureRound
import com.example.hellocompose.presentation.components.ChatInput
import kotlinx.coroutines.flow.collectLatest

// Цвета и мета-информация для каждого уровня температуры
private data class TempMeta(
    val emoji: String,
    val label: String,
    val description: String,
    val color: Color
)

private fun tempMeta(temp: Float): TempMeta = when {
    temp <= 0.05f -> TempMeta("🧊", "0.0", "Точный", Color(0xFF1565C0))
    temp <= 0.75f -> TempMeta("⚖️", "0.7", "Сбалансированный", Color(0xFF2E7D32))
    else          -> TempMeta("🔥", "1.2", "Креативный", Color(0xFFBF360C))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureChatScreen(
    viewModel: TemperatureChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TemperatureChatEffect.ScrollToBottom -> {
                    if (state.rounds.isNotEmpty()) {
                        listState.animateScrollToItem(state.rounds.lastIndex)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌡 Температура") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    if (state.rounds.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.handleIntent(TemperatureChatIntent.ClearHistory)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
            // Шапка с тремя чипами температур (всегда видна, только для наглядности)
            TemperatureChipsRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (state.rounds.isEmpty()) {
                    item {
                        EmptyTemperatureScreen(
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }

                itemsIndexed(
                    items = state.rounds,
                    key = { _, round -> round.id }
                ) { _, round ->
                    TemperatureRoundItem(round = round)
                }
            }

            ChatInput(
                inputText = state.inputText,
                isLoading = state.isAnyLoading,
                onTextChange = { viewModel.handleIntent(TemperatureChatIntent.TypeMessage(it)) },
                onSendClick = { viewModel.handleIntent(TemperatureChatIntent.SendMessage) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chips row — наглядное отображение трёх температур
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemperatureChipsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(0f, 0.7f, 1.2f).forEach { temp ->
            val meta = tempMeta(temp)
            Surface(
                shape = RoundedCornerShape(50),
                color = meta.color.copy(alpha = 0.12f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = meta.color.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = "${meta.emoji} ${meta.label}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = meta.color,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyTemperatureScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🧊⚖️🔥", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Задайте вопрос",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Один и тот же запрос\nбудет отправлен при\ntemperature 0.0, 0.7 и 1.2",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Один раунд: вопрос + три карточки ответов
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemperatureRoundItem(
    round: TemperatureRound,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Вопрос пользователя — пузырь справа
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = round.question,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = 16.dp, bottomEnd = 4.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Три карточки ответов (стопкой)
        round.responses.forEach { response ->
            TemperatureResponseCard(response = response)
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Карточка ответа для одной температуры
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemperatureResponseCard(
    response: TemperatureResponse,
    modifier: Modifier = Modifier
) {
    val meta = tempMeta(response.temperature)
    val accentColor = meta.color
    val bgColor = if (response.isError)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    else
        accentColor.copy(alpha = 0.07f)
    val borderColor = if (response.isError)
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    else
        accentColor.copy(alpha = 0.3f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    topStart = 4.dp, topEnd = 16.dp,
                    bottomStart = 16.dp, bottomEnd = 16.dp
                )
            ),
        shape = RoundedCornerShape(
            topStart = 4.dp, topEnd = 16.dp,
            bottomStart = 16.dp, bottomEnd = 16.dp
        ),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Шапка: эмодзи в кружочке + название + описание
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = meta.emoji,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "temperature = ${meta.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = meta.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Тело: загрузка или текст
            if (response.isLoading) {
                TemperatureLoadingDots(accentColor = accentColor)
            } else {
                Text(
                    text = response.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (response.isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Анимированные точки загрузки (цвет соответствует температуре)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TemperatureLoadingDots(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "temp_loading")
    Row(
        modifier = modifier.padding(vertical = 4.dp),
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
                    .background(accentColor.copy(alpha = 0.6f))
            )
        }
    }
}
