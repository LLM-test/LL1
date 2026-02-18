package com.example.hellocompose.presentation.expert

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.hellocompose.domain.model.ExpertCharacter
import com.example.hellocompose.domain.model.ExpertCharacters
import com.example.hellocompose.domain.model.ExpertMessage
import com.example.hellocompose.domain.model.ExpertResponse
import com.example.hellocompose.presentation.components.ChatInput
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertChatScreen(
    viewModel: ExpertChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ExpertChatEffect.ScrollToBottom -> {
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
                title = {
                    Column {
                        Text("Эксперты")
                        if (!state.showCharacterPicker && state.selectedCharacters.isNotEmpty()) {
                            Text(
                                text = state.selectedCharacters.joinToString(" ") { it.emoji },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
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
                    if (!state.showCharacterPicker) {
                        IconButton(onClick = {
                            viewModel.handleIntent(ExpertChatIntent.ResetCharacters)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Сменить экспертов",
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
            if (state.showCharacterPicker) {
                CharacterPickerSection(
                    selected = state.selectedCharacters,
                    onToggle = { viewModel.handleIntent(ExpertChatIntent.ToggleCharacter(it)) },
                    onConfirm = { viewModel.handleIntent(ExpertChatIntent.ConfirmCharacters) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (state.messages.isEmpty()) {
                        item {
                            EmptyExpertChat(
                                characters = state.selectedCharacters,
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }

                    itemsIndexed(
                        items = state.messages,
                        key = { _, msg -> msg.id }
                    ) { _, message ->
                        ExpertMessageItem(message = message)
                    }
                }

                ChatInput(
                    inputText = state.inputText,
                    isLoading = state.isAnyLoading,
                    onTextChange = { viewModel.handleIntent(ExpertChatIntent.TypeMessage(it)) },
                    onSendClick = { viewModel.handleIntent(ExpertChatIntent.SendMessage) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Character picker
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CharacterPickerSection(
    selected: List<ExpertCharacter>,
    onToggle: (ExpertCharacter) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Выберите экспертов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Каждый ответит на ваш вопрос по-своему",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpertCharacters.ALL.forEach { character ->
                val isSelected = selected.any { it.id == character.id }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(character) },
                    label = {
                        Text("${character.emoji} ${character.name}")
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }

        if (selected.isNotEmpty()) {
            Text(
                text = "Выбрано: ${selected.size} — ${selected.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Начать диалог с экспертами")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyExpertChat(
    characters: List<ExpertCharacter>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = characters.map { it.emoji }.joinToString(" "),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Задайте вопрос",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Все выбранные эксперты\nответят одновременно",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message item: question bubble + expert response cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpertMessageItem(
    message: ExpertMessage,
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
                text = message.question,
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

        // Карточки ответов экспертов
        message.responses.forEach { response ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                ExpertResponseCard(response = response)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single expert response card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpertResponseCard(
    response: ExpertResponse,
    modifier: Modifier = Modifier
) {
    val character = response.character
    val isError = response.isError
    val bgColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

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
            // Шапка с именем персонажа
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Эмодзи в кружочке
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = character.emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Тело: загрузка или текст
            if (response.isLoading) {
                ExpertLoadingDots()
            } else {
                Text(
                    text = response.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading dots (inside expert card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpertLoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "expert_loading")
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
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
            )
        }
    }
}
