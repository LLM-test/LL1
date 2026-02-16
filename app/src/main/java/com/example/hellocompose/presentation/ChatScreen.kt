package com.example.hellocompose.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hellocompose.presentation.components.ChatInput
import com.example.hellocompose.presentation.components.LoadingBubble
import com.example.hellocompose.presentation.components.MessageBubble
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

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
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = state.messages,
                    key = { "${it.timestamp}_${it.role}" }
                ) { message ->
                    MessageBubble(message = message)
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
}
