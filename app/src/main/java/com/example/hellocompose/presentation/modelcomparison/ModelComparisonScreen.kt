package com.example.hellocompose.presentation.modelcomparison

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
import com.example.hellocompose.domain.model.JudgeVerdict
import com.example.hellocompose.domain.model.ModelComparisonResponse
import com.example.hellocompose.domain.model.ModelComparisonRound
import com.example.hellocompose.domain.model.ModelConfig
import com.example.hellocompose.domain.model.ModelConfigs
import com.example.hellocompose.presentation.components.ChatInput
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelComparisonScreen(
    viewModel: ModelComparisonViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ModelComparisonEffect.ScrollToBottom -> {
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
                title = { Text("⚖️ Модели") },
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
                            viewModel.handleIntent(ModelComparisonIntent.ClearHistory)
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
            // Шапка с тремя чипами моделей
            ModelChipsRow(
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
                        EmptyModelComparisonScreen(
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }

                itemsIndexed(
                    items = state.rounds,
                    key = { _, round -> round.id }
                ) { _, round ->
                    ModelComparisonRoundItem(round = round)
                }
            }

            ChatInput(
                inputText = state.inputText,
                isLoading = state.isAnyLoading,
                onTextChange = { viewModel.handleIntent(ModelComparisonIntent.TypeMessage(it)) },
                onSendClick = { viewModel.handleIntent(ModelComparisonIntent.SendMessage) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chips row — три модели
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModelChipsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModelConfigs.ALL.forEachIndexed { index, cfg ->
            val color = Color(cfg.accentColor)
            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = "${index + 1}. ${cfg.emoji} ${cfg.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyModelComparisonScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🐥🦙🧠", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Сравнение моделей",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Один вопрос отправляется\nпараллельно трём моделям.\nСравни скорость, токены и стоимость.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Один раунд: вопрос + три карточки
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModelComparisonRoundItem(
    round: ModelComparisonRound,
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

        // Карточки ответов (стопкой)
        round.responses.forEachIndexed { index, response ->
            ModelResponseCard(response = response, index = index)
            Spacer(Modifier.height(6.dp))
        }

        // Карточка судьи — появляется после всех трёх ответов
        round.judgeVerdict?.let { verdict ->
            JudgeVerdictCard(verdict = verdict)
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Карточка ответа одной модели
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModelResponseCard(
    response: ModelComparisonResponse,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    val cfg = response.modelConfig
    val accentColor = Color(cfg.accentColor)
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
            // Шапка: эмодзи в кружке + название + уровень
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cfg.emoji,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "${index + 1}. ${cfg.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = cfg.tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Тело: загрузка или текст ответа
            if (response.isLoading) {
                ModelLoadingDots(accentColor = accentColor)
            } else {
                Text(
                    text = response.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (response.isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                // MetricsBar — только если не ошибка и есть данные
                if (!response.isError && (response.elapsedMs > 0 || response.promptTokens > 0)) {
                    Spacer(Modifier.height(8.dp))
                    MetricsBar(response = response, accentColor = accentColor)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MetricsBar — время / токены / стоимость
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricsBar(
    response: ModelComparisonResponse,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val totalTokens = response.promptTokens + response.completionTokens
    val costText = when {
        response.costUsd == 0.0   -> "$0.0000"
        response.costUsd < 0.0001 -> "<$0.0001"
        else                       -> "$" + "%.4f".format(response.costUsd)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⏱ ${response.elapsedMs}ms",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )
        Text(
            text = "🔤 $totalTokens tok",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )
        Text(
            text = "💰 $costText",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Карточка судьи — оценивает качество всех трёх ответов
// ─────────────────────────────────────────────────────────────────────────────

private val judgeColor = Color(0xFFF57F17) // янтарный/золотой

@Composable
private fun JudgeVerdictCard(
    verdict: JudgeVerdict,
    modifier: Modifier = Modifier
) {
    val bgColor = if (verdict.isError)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    else
        judgeColor.copy(alpha = 0.07f)
    val borderColor = if (verdict.isError)
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    else
        judgeColor.copy(alpha = 0.35f)

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
            // Шапка
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(judgeColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏅", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "Судья",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = judgeColor
                    )
                    Text(
                        text = "сравнение качества ответов",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (verdict.isLoading) {
                ModelLoadingDots(accentColor = judgeColor)
            } else {
                Text(
                    text = verdict.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (verdict.isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Анимированные точки загрузки
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModelLoadingDots(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "model_loading")
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
