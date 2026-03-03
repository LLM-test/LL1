package com.example.hellocompose.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hellocompose.presentation.components.ChatInput
import com.example.hellocompose.presentation.components.LoadingBubble
import com.example.hellocompose.presentation.components.MessageBubble
import com.example.hellocompose.presentation.components.QuizBottomSheet
import com.example.hellocompose.presentation.components.QuizMessageBubble
import com.example.hellocompose.presentation.components.SettingsBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatSideMenu(
                onNavigateToAgent           = { scope.launch { drawerState.close() }; onNavigateToAgent() },
                onNavigateToModelComparison = { scope.launch { drawerState.close() }; onNavigateToModelComparison() },
                onNavigateToTemperature     = { scope.launch { drawerState.close() }; onNavigateToTemperature() },
                onNavigateToExperts         = { scope.launch { drawerState.close() }; onNavigateToExperts() },
                onOpenQuiz                  = { scope.launch { drawerState.close() }; showQuiz = true },
                onOpenSettings              = { scope.launch { drawerState.close() }; showSettings = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DeepSeek Chat") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Меню",
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
                        item { LoadingBubble() }
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
    } // ModalNavigationDrawer
}

// ─────────────────────────────────────────────────────────────────────────────
// Боковое меню
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatSideMenu(
    onNavigateToAgent: () -> Unit,
    onNavigateToModelComparison: () -> Unit,
    onNavigateToTemperature: () -> Unit,
    onNavigateToExperts: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val headerBg = MaterialTheme.colorScheme.primaryContainer
    val headerFg = MaterialTheme.colorScheme.onPrimaryContainer

    ModalDrawerSheet {
        // Заголовок
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text("DeepSeek Chat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = headerFg)
            Spacer(Modifier.height(2.dp))
            Text("Выберите режим", fontSize = 12.sp, color = headerFg.copy(alpha = 0.7f))
        }

        Spacer(Modifier.height(8.dp))

        DrawerNavItem(
            icon        = Icons.Default.SmartToy,
            label       = "Агент",
            description = "Диалог с инструментами и памятью",
            onClick     = onNavigateToAgent
        )
        DrawerNavItem(
            icon        = Icons.AutoMirrored.Filled.CompareArrows,
            label       = "Сравнение моделей",
            description = "Три модели + судья качества",
            onClick     = onNavigateToModelComparison
        )
        DrawerNavItem(
            icon        = Icons.Default.Thermostat,
            label       = "Температура",
            description = "Влияние параметра temperature",
            onClick     = onNavigateToTemperature
        )
        DrawerNavItem(
            icon        = Icons.Default.Groups,
            label       = "Эксперты",
            description = "Диалог с персонажами",
            onClick     = onNavigateToExperts
        )

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

        DrawerNavItem(
            icon        = Icons.Default.Quiz,
            label       = "Викторина",
            description = "Квиз на любую тему",
            onClick     = onOpenQuiz
        )
        DrawerNavItem(
            icon        = Icons.Default.Tune,
            label       = "Настройки",
            description = "Модель, температура, промпт",
            onClick     = onOpenSettings
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon     = { Icon(icon, contentDescription = null) },
        label    = {
            Column {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        },
        selected = false,
        onClick  = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
