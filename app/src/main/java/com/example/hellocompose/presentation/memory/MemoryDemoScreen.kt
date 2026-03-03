package com.example.hellocompose.presentation.memory

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest

// ── Цвета ─────────────────────────────────────────────────────────────────────

private val shortTermColor = Color(0xFF455A64) // blue-grey
private val workingColor   = Color(0xFF1565C0) // blue
private val longTermColor  = Color(0xFF2E7D32) // green

private fun scenarioColor(index: Int) = when (index) {
    0    -> shortTermColor
    1    -> workingColor
    else -> longTermColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDemoScreen(
    viewModel: MemoryDemoViewModel,
    onNavigateBack: () -> Unit
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MemoryDemoEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧪 Демо памяти", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state.steps.isNotEmpty()) {
                        IconButton(onClick = { viewModel.handleIntent(MemoryDemoIntent.ResetScenario) }) {
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
                DemoScenario.entries.forEach { scenario ->
                    Tab(
                        selected = state.activeTab == scenario.tabIndex,
                        onClick  = {
                            if (!state.isRunning)
                                viewModel.handleIntent(MemoryDemoIntent.SelectTab(scenario.tabIndex))
                        },
                        text = {
                            Text("${scenario.icon} ${scenario.title}", fontSize = 11.sp)
                        }
                    )
                }
            }

            val scenario = DemoScenario.entries[state.activeTab]
            val accentColor = scenarioColor(state.activeTab)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                // Заголовок сценария
                item {
                    ScenarioHeader(scenario = scenario, accentColor = accentColor)
                }

                // Кнопка запуска / стоп
                item {
                    RunButton(
                        isRunning  = state.isRunning,
                        hasSteps   = state.steps.isNotEmpty(),
                        isDone     = state.conclusion != null,
                        accentColor = accentColor,
                        onRun  = { viewModel.handleIntent(MemoryDemoIntent.RunScenario) },
                        onStop = { viewModel.handleIntent(MemoryDemoIntent.StopScenario) },
                        onReset = { viewModel.handleIntent(MemoryDemoIntent.ResetScenario) }
                    )
                }

                // Шаги
                if (state.steps.isNotEmpty()) {
                    item {
                        Text("Шаги", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    itemsIndexed(state.steps) { i, step ->
                        StepRow(index = i, step = step, total = state.steps.size)
                    }
                }

                // System prompt preview
                if (state.steps.isNotEmpty()) {
                    item {
                        SystemPromptPanel(preview = state.systemPromptMemoryBlock)
                    }
                }

                // До / После
                if (state.beforeResponse != null || state.afterResponse != null) {
                    item {
                        BeforeAfterPanel(
                            scenario      = scenario,
                            accentColor   = accentColor,
                            beforeResponse = state.beforeResponse,
                            afterResponse  = state.afterResponse,
                            steps          = state.steps
                        )
                    }
                }

                // Вывод
                if (state.conclusion != null) {
                    item {
                        ConclusionCard(text = state.conclusion!!, accentColor = accentColor)
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Заголовок сценария ────────────────────────────────────────────────────────

@Composable
private fun ScenarioHeader(scenario: DemoScenario, accentColor: Color) {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = accentColor.copy(alpha = 0.08f),
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
                Text(scenario.title, fontWeight = FontWeight.Bold, color = accentColor,
                    style = MaterialTheme.typography.titleSmall)
                Text(scenario.subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(if (hasSteps) "▶ Продолжить" else "▶ Запустить демо")
                }
            }
        }
    }
}

// ── Строка шага ───────────────────────────────────────────────────────────────

@Composable
private fun StepRow(index: Int, step: DemoStep, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Иконка статуса
        when (step.status) {
            StepStatus.DONE -> Icon(
                Icons.Default.CheckCircle, contentDescription = null,
                tint = Color(0xFF43A047), modifier = Modifier.size(20.dp)
            )
            StepStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            StepStatus.PENDING -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${index + 1}/$total — ${step.description}",
                style = MaterialTheme.typography.bodySmall,
                color = if (step.status == StepStatus.PENDING)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.onSurface
            )
            // Badge источника памяти
            if (step.status == StepStatus.DONE && step.memoryBadge != null) {
                val badgeColor = when {
                    step.memoryBadge.startsWith("❌") -> Color(0xFFB71C1C)
                    step.memoryBadge.startsWith("💬") -> shortTermColor
                    step.memoryBadge.startsWith("🔧") -> workingColor
                    else -> longTermColor
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                ) {
                    Text(
                        step.memoryBadge,
                        fontSize = 10.sp,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                Text("📋 System prompt (блок памяти)",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f))
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
                    text = if (preview.isEmpty()) "(память пуста — в system prompt ничего не добавлено)"
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
    scenario: DemoScenario,
    accentColor: Color,
    beforeResponse: String?,
    afterResponse: String?,
    steps: List<DemoStep>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сравнение ответов", fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        // Определяем badges из шагов
        val beforeBadge = steps.firstOrNull { it.isBeforeStep }?.memoryBadge
        val afterBadge  = steps.firstOrNull { it.isAfterStep }?.memoryBadge

        if (beforeResponse != null) {
            ResponseCard(
                label       = "ДО",
                badge       = beforeBadge,
                response    = beforeResponse,
                borderColor = accentColor
            )
        }
        if (afterResponse != null) {
            val isLongTermScenario = scenario == DemoScenario.LONG_TERM
            ResponseCard(
                label       = if (isLongTermScenario) "РЕЗУЛЬТАТ" else "ПОСЛЕ",
                badge       = afterBadge,
                response    = afterResponse,
                borderColor = if (afterBadge?.startsWith("❌") == true) Color(0xFFB71C1C) else accentColor
            )
        }
    }
}

@Composable
private fun ResponseCard(label: String, badge: String?, response: String, borderColor: Color) {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = borderColor.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = borderColor,
                    style = MaterialTheme.typography.labelMedium)
                if (badge != null) {
                    val badgeColor = when {
                        badge.startsWith("❌") -> Color(0xFFB71C1C)
                        badge.startsWith("💬") -> shortTermColor
                        badge.startsWith("🔧") -> workingColor
                        else -> longTermColor
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.12f),
                        modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                    ) {
                        Text(badge, fontSize = 10.sp, color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
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
        shape  = RoundedCornerShape(12.dp),
        color  = accentColor.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("✅ Вывод", fontWeight = FontWeight.Bold, color = accentColor,
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp)
        }
    }
}
