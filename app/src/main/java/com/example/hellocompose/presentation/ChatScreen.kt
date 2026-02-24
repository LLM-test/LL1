package com.example.hellocompose.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hellocompose.presentation.components.ChatInput
import com.example.hellocompose.presentation.components.LoadingBubble
import com.example.hellocompose.presentation.components.MessageBubble
import com.example.hellocompose.presentation.components.QuizBottomSheet
import com.example.hellocompose.presentation.components.QuizMessageBubble
import com.example.hellocompose.presentation.components.SettingsBottomSheet
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToExperts: () -> Unit = {},
    onNavigateToTemperature: () -> Unit = {},
    onNavigateToModelComparison: () -> Unit = {},
    onNavigateToAgent: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    var showSettings by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val quizSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChatEffect.ScrollToBottom -> {
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
                title = { Text("DeepSeek Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToAgent) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Агент",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onNavigateToModelComparison) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Сравнение моделей",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onNavigateToTemperature) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = "Температура",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onNavigateToExperts) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Эксперты",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { showQuiz = true }) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = "Викторина",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = state.messages,
                    key = { index, message -> "${index}_${message.timestamp}_${message.role}" }
                ) { _, message ->
                    if (message.quizData != null) {
                        QuizMessageBubble(
                            quizData = message.quizData,
                            selectedOption = message.selectedOption,
                            localScore = state.localScore,
                            onOptionSelected = if (!state.isLoading && message.selectedOption == null) { key, text ->
                                viewModel.handleIntent(ChatIntent.SelectQuizOption(key, text))
                            } else null
                        )
                    } else {
                        MessageBubble(message = message)
                    }
                }

                if (state.isLoading) {
                    item {
                        LoadingBubble()
                    }
                }
            }

            ChatInput(
                inputText = state.inputText,
                isLoading = state.isLoading,
                onTextChange = { viewModel.handleIntent(ChatIntent.TypeMessage(it)) },
                onSendClick = { viewModel.handleIntent(ChatIntent.SendMessage) }
            )
        }
    }

    // Шторка настроек
    if (showSettings) {
        SettingsBottomSheet(
            settings = state.settings,
            sheetState = settingsSheetState,
            onDismiss = { showSettings = false },
            onApply = { newSettings ->
                viewModel.handleIntent(ChatIntent.UpdateSettings(newSettings))
            }
        )
    }

    // Шторка викторины
    if (showQuiz) {
        QuizBottomSheet(
            sheetState = quizSheetState,
            onDismiss = { showQuiz = false },
            onStart = { config ->
                viewModel.handleIntent(ChatIntent.StartQuiz(config))
            }
        )
    }
}
