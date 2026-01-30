package com.azime.aitestapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp

/**
 * Main chat screen composable.
 *
 * ## UI Architecture:
 *
 * The screen is structured as:
 * ```
 * ┌────────────────────────────┐
 * │  Model Status Banner       │  ← Shows download/error status
 * ├────────────────────────────┤
 * │                            │
 * │  Message List (LazyColumn) │  ← Scrollable message history
 * │    - User bubbles (right)  │
 * │    - AI bubbles (left)     │
 * │    - Typing indicator      │
 * │                            │
 * ├────────────────────────────┤
 * │  Input Bar                 │  ← TextField + Send button
 * └────────────────────────────┘
 * ```
 *
 * ## Performance Considerations:
 *
 * - LazyColumn only renders visible items
 * - Auto-scroll uses LaunchedEffect to avoid animation jank
 * - Typing indicator uses infinite animation efficiently
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            ModelStatusBanner(modelStatus)
        },
        bottomBar = {
            InputBar(
                inputText = inputText,
                onInputChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                isEnabled = modelStatus is ModelStatus.Ready && uiState !is ChatUiState.Generating
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                EmptyStateMessage(modelStatus)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isStreaming = message.isStreaming && uiState is ChatUiState.Generating
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner showing model status (downloading, error, etc.)
 */
@Composable
private fun ModelStatusBanner(status: ModelStatus) {
    val backgroundColor: Color
    val contentColor: Color
    val text: String
    val showProgress: Boolean
    val progressValue: Float?

    when (status) {
        is ModelStatus.Checking -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            text = "Checking AI availability..."
            showProgress = true
            progressValue = null
        }
        is ModelStatus.Downloading -> {
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            text = "Downloading AI model... ${status.progress}%"
            showProgress = true
            progressValue = status.progress / 100f
        }
        is ModelStatus.Ready -> {
            // Don't show banner when ready
            return
        }
        is ModelStatus.Error -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            text = "Error: ${status.message}"
            showProgress = false
            progressValue = null
        }
        is ModelStatus.Unavailable -> {
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            text = status.reason
            showProgress = false
            progressValue = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (status) {
                    is ModelStatus.Downloading -> Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    is ModelStatus.Error, is ModelStatus.Unavailable -> Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (showProgress && progressValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Empty state message when no chat history exists.
 */
@Composable
private fun EmptyStateMessage(modelStatus: ModelStatus) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "On-Device AI Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (modelStatus) {
                    is ModelStatus.Ready -> "Type a message to start chatting!\nAll responses are generated on your device."
                    is ModelStatus.Downloading -> "AI model is downloading...\nThis only happens once."
                    is ModelStatus.Checking -> "Checking AI availability..."
                    is ModelStatus.Error -> "Failed to load AI model.\nPlease restart the app."
                    is ModelStatus.Unavailable -> "On-device AI is not available on this device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual message bubble with role-based styling.
 *
 * User messages: Right-aligned, primary color
 * AI messages: Left-aligned, surface variant color
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean
) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Role label
                Text(
                    text = if (isUser) "You" else "AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Message content
                if (message.content.isNotEmpty()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Typing indicator for streaming messages
                AnimatedVisibility(
                    visible = isStreaming && !isUser,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TypingIndicator(
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated typing indicator (three bouncing dots).
 *
 * Uses infinite animation with staggered timing for natural feel.
 */
@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * Input bar with text field and send button.
 */
@Composable
private fun InputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        text = if (isEnabled) "Type a message..." else "Waiting for model...",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                enabled = isEnabled,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            IconButton(
                onClick = onSend,
                enabled = isEnabled && inputText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled && inputText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isEnabled && inputText.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}