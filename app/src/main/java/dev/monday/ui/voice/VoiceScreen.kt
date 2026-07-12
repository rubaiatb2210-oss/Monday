package dev.monday.ui.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.monday.data.speech.ListeningState
import dev.monday.ui.theme.*

@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val listeningState by viewModel.listeningState.collectAsState()
    val speakingState by viewModel.speakingState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val exchanges by viewModel.exchanges.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new exchange added
    LaunchedEffect(exchanges.size) {
        if (exchanges.isNotEmpty()) {
            listState.animateScrollToItem(exchanges.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MondayNavy)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Voice",
                style = MaterialTheme.typography.headlineMedium,
                color = MondayTextPrimary
            )
        }

        // Conversation history
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (exchanges.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = MondayTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tap the microphone to speak",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MondayTextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(exchanges) { exchange ->
                VoiceExchangeItem(exchange)
            }
        }

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MondayRed,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // Status text
        val statusText = when {
            listeningState == ListeningState.LISTENING -> "Listening…"
            listeningState == ListeningState.PROCESSING -> "Processing…"
            speakingState -> "Monday is speaking…"
            isProcessing -> "Thinking…"
            else -> "Tap to speak"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                listeningState == ListeningState.LISTENING -> MondayBlue
                speakingState -> MondayGreen
                else -> MondayTextTertiary
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        // Live transcription
        val partialTranscription by viewModel.partialTranscription.collectAsState()
        
        partialTranscription?.let { text ->
            if (text.isNotBlank() && listeningState == ListeningState.LISTENING) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MondayTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Mic button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            MicButton(
                listeningState = listeningState,
                speakingState = speakingState,
                onClick = {
                    if (speakingState) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.toggleListening()
                    }
                }
            )
        }
    }
}

@Composable
private fun MicButton(
    listeningState: ListeningState,
    speakingState: Boolean,
    onClick: () -> Unit
) {
    val isActive = listeningState == ListeningState.LISTENING

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 0.4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MondayBlue.copy(alpha = glowAlpha))
            )
        }

        // Button
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            containerColor = when {
                isActive -> MondayBlue
                speakingState -> MondayGreen
                else -> MondayNavyCard
            },
            contentColor = MondayTextPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isActive) 12.dp else 6.dp
            )
        ) {
            Icon(
                imageVector = when {
                    speakingState -> Icons.Filled.Stop
                    isActive -> Icons.Filled.MicOff
                    else -> Icons.Filled.Mic
                },
                contentDescription = "Microphone",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun VoiceExchangeItem(exchange: VoiceExchange) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User message — right aligned
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                color = MondayBlue.copy(alpha = 0.15f)
            ) {
                Text(
                    text = exchange.userText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MondayBlueBright,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monday response — left aligned
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (exchange.isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MondayBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thinking…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MondayTextTertiary
                    )
                }
            } else if (exchange.mondayResponse != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    color = MondayNavySurface
                ) {
                    Text(
                        text = exchange.mondayResponse,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (exchange.isError) MondayRedLight else MondayTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
