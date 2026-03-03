package com.example.hellocompose.presentation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Цвета сценариев ───────────────────────────────────────────────────────────

private val noProfileColor   = Color(0xFFE65100) // orange — нет профиля
private val juniorSeniorColor = Color(0xFF283593) // indigo — уровни
private val constraintsColor  = Color(0xFF004D40) // teal — ограничения

private fun scenarioColor(index: Int) = when (index) {
    0    -> noProfileColor
    1    -> juniorSeniorColor
    else -> constraintsColor
}

private fun badgeColor(badge: String): Color = when {
    badge.startsWith("🚫") -> Color(0xFFB71C1C)
    badge.startsWith("👶") -> Color(0xFF1B5E20)
    badge.startsWith("👨") -> juniorSeniorColor
    badge.startsWith("⚙") -> Color(0xFF4A148C)
    badge.startsWith("🔒") -> constraintsColor
    else                   -> Color(0xFF37474F)
}

// ── Экран ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDemoScreen(
    viewModel: ProfileDemoViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎭 Демо профиля", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state.steps.isNotEmpty()) {
                        IconButton(onClick = { viewModel.handleIntent(ProfileDemoIntent.ResetScenario) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Сбросить")
                        }
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
        ) {
            // Вкладки сценариев
            TabRow(selectedTabIndex = state.activeTab) {
                ProfileDemoScenario.entries.forEach { scenario ->
                    Tab(
                        selected = state.activeTab == scenario.tabIndex,
                        onClick  = {
                            if (!state.isRunning)
                                viewModel.handleIntent(ProfileDemoIntent.SelectTab(scenario.tabIndex))
                        },
                        text = { Text("${scenario.icon} ${scenario.title}", fontSize = 11.sp) }
                    )
                }
            }

            val scenario    = ProfileDemoScenario.entries[state.activeTab]
            val accentColor = scenarioColor(state.activeTab)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Заголовок сценария
                item { ScenarioHeader(scenario = scenario, accentColor = accentColor) }

                // Кнопка запуска / стоп
                item {
                    RunButton(
                        isRunning   = state.isRunning,
                        hasSteps    = state.steps.isNotEmpty(),
                        isDone      = state.conclusion != null,
                        accentColor = accentColor,
                        onRun       = { viewModel.handleIntent(ProfileDemoIntent.RunScenario) },
                        onStop      = { viewModel.handleIntent(ProfileDemoIntent.StopScenario) },
                        onReset     = { viewModel.handleIntent(ProfileDemoIntent.ResetScenario) }
                    )
                }

                // Шаги
                if (state.steps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Шаги",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    itemsIndexed(state.steps) { i, step ->
                        StepRow(index = i, step = step, total = state.steps.size)
                    }
                }

                // System prompt preview
                if (state.steps.isNotEmpty()) {
                    item { SystemPromptPanel(preview = state.systemPromptPreview) }
                }

                // До / После
                if (state.beforeResponse != null || state.afterResponse != null) {
                    item {
                        BeforeAfterPanel(
                            scenario       = scenario,
                            accentColor    = accentColor,
                            beforeResponse = state.beforeResponse,
                            afterResponse  = state.afterResponse,
                            steps          = state.steps
                        )
                    }
                }

                // Вывод
                if (state.conclusion != null) {
                    item { ConclusionCard(text = state.conclusion!!, accentColor = accentColor) }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Заголовок сценария ────────────────────────────────────────────────────────

@Composable
private fun ScenarioHeader(scenario: ProfileDemoScenario, accentColor: Color) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = accentColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(scenario.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    scenario.title, fontWeight = FontWeight.Bold, color = accentColor,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    scenario.subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Вопрос: «${scenario.question}»",
                    fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ── Кнопка запуска ────────────────────────────────────────────────────────────

@Composable
private fun RunButton(
    isRunning: Boolean,
    hasSteps: Boolean,
    isDone: Boolean,
    accentColor: Color,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            isRunning -> {
                Button(
                    onClick = onStop,
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("⏹ Остановить")
                }
            }
            isDone -> {
                OutlinedButton(onClick = onReset) { Text("🔄 Запустить снова") }
            }
            else -> {
                Button(
                    onClick = onRun,
                    colors  = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(if (hasSteps) "▶ Продолжить" else "▶ Запустить демо")
                }
            }
        }
    }
}

// ── Строка шага ───────────────────────────────────────────────────────────────

@Composable
private fun StepRow(index: Int, step: ProfileDemoStep, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Иконка статуса
        when (step.status) {
            ProfileStepStatus.DONE    -> Icon(
                Icons.Default.CheckCircle, contentDescription = null,
                tint = Color(0xFF43A047), modifier = Modifier.size(20.dp)
            )
            ProfileStepStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            ProfileStepStatus.PENDING -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${index + 1}/$total — ${step.description}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (step.status == ProfileStepStatus.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
                color = if (step.status == ProfileStepStatus.PENDING)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.onSurface
            )

            if (step.status == ProfileStepStatus.DONE) {
                // Операция с профилем (monospace)
                if (step.profileAction != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            step.profileAction,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                // Запрос пользователя
                if (step.userMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("👤", fontSize = 11.sp)
                            Text(
                                step.userMessage,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Ответ агента
                if (step.agentResponse != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🤖", fontSize = 11.sp)
                            Text(
                                step.agentResponse,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Badge пресета / режима
                if (step.profileBadge != null) {
                    val bc = badgeColor(step.profileBadge)
                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = bc.copy(alpha = 0.12f),
                        modifier = Modifier.border(1.dp, bc.copy(alpha = 0.3f), RoundedCornerShape(50))
                    ) {
                        Text(
                            step.profileBadge,
                            fontSize = 10.sp,
                            color    = bc,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── System prompt preview ─────────────────────────────────────────────────────

@Composable
private fun SystemPromptPanel(preview: String) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📋 System prompt (текущий профиль)",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Divider()
                Text(
                    text = if (preview.isEmpty()) "(профиль пустой — блок не добавляется в system prompt)"
                           else preview,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 11.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// ── До / После ────────────────────────────────────────────────────────────────

@Composable
private fun BeforeAfterPanel(
    scenario: ProfileDemoScenario,
    accentColor: Color,
    beforeResponse: String?,
    afterResponse: String?,
    steps: List<ProfileDemoStep>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Сравнение ответов",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        val beforeBadge = steps.firstOrNull { it.isBeforeStep }?.profileBadge
        val afterBadge  = steps.firstOrNull { it.isAfterStep }?.profileBadge

        if (beforeResponse != null) {
            ResponseCard(
                label       = "ДО",
                badge       = beforeBadge,
                response    = beforeResponse,
                borderColor = accentColor
            )
        }
        if (afterResponse != null) {
            val afterColor = if (afterBadge != null) badgeColor(afterBadge) else accentColor
            ResponseCard(
                label       = "ПОСЛЕ",
                badge       = afterBadge,
                response    = afterResponse,
                borderColor = afterColor
            )
        }
    }
}

@Composable
private fun ResponseCard(label: String, badge: String?, response: String, borderColor: Color) {
    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = borderColor.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    color = borderColor,
                    style = MaterialTheme.typography.labelMedium
                )
                if (badge != null) {
                    val bc = badgeColor(badge)
                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = bc.copy(alpha = 0.12f),
                        modifier = Modifier.border(1.dp, bc.copy(alpha = 0.3f), RoundedCornerShape(50))
                    ) {
                        Text(
                            badge, fontSize = 10.sp, color = bc,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(response, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Вывод ─────────────────────────────────────────────────────────────────────

@Composable
private fun ConclusionCard(text: String, accentColor: Color) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = accentColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "✅ Вывод",
                fontWeight = FontWeight.Bold,
                color = accentColor,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
        }
    }
}
