package com.example.hellocompose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.hellocompose.domain.model.ChatMessage

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatMessage.Role.USER
    val isError = message.role == ChatMessage.Role.ERROR
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = message.content,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        )
    }
}
