package com.fluxplayer.ui.player.state

import android.net.Uri

data class PlayerState(
    val uri: Uri? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isEnded: Boolean = false,
    val errorMessage: String? = null,
    val controlsVisible: Boolean = true
) {
    val progress: Float
        get() = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val remainingMs: Long
        get() = (durationMs - currentPositionMs).coerceAtLeast(0L)
}

