package com.fluxplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fluxplayer.ui.player.components.PlayerControls
import com.fluxplayer.ui.player.components.PlayerSurface

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Surface
        PlayerSurface(
            exoPlayer = viewModel.exoPlayer,
            onTap = { viewModel.toggleControls() }
        )

        // Overlay Controls
        PlayerControls(
            playerState = playerState,
            onPlayPauseToggle = { viewModel.togglePlayPause() },
            onSeek = { positionMs -> viewModel.seekTo(positionMs) },
            onSeekBy = { deltaMs -> viewModel.seekBy(deltaMs) },
            onBackClick = onBackClick,
            onOpenFileClick = onOpenFileClick,
            onResetAutoHideTimer = { viewModel.hideControls() }
        )
    }
}

