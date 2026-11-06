package com.fluxplayer.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxplayer.ui.player.state.PlayerState
import com.fluxplayer.ui.theme.OverlayBackground
import com.fluxplayer.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay

@Composable
fun PlayerControls(
    playerState: PlayerState,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onBackClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onResetAutoHideTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-hide controls after 4 seconds if playing
    LaunchedEffect(playerState.controlsVisible, playerState.isPlaying) {
        if (playerState.controlsVisible && playerState.isPlaying) {
            delay(4000)
            onResetAutoHideTimer()
        }
    }

    AnimatedVisibility(
        visible = playerState.controlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayBackground)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = playerState.title.ifEmpty { "FluxPlayer" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = onOpenFileClick) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open File",
                        tint = Color.White
                    )
                }
            }

            // Center Play / Pause / Seek Controls
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek Backward 10s
                IconButton(
                    onClick = { onSeekBy(-10_000L) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause / Buffering Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = PrimaryTeal,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier
                                .size(72.dp)
                                .background(PrimaryTeal, CircleShape)
                        ) {
                            val icon = when {
                                playerState.isEnded -> Icons.Default.Replay
                                playerState.isPlaying -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Seek Forward 10s
                IconButton(
                    onClick = { onSeekBy(10_000L) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom Bar: TimeBar + Error Message (if any)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                if (playerState.errorMessage != null) {
                    Text(
                        text = playerState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                TimeBar(
                    currentPositionMs = playerState.currentPositionMs,
                    durationMs = playerState.durationMs,
                    onSeek = onSeek
                )
            }
        }
    }
}

