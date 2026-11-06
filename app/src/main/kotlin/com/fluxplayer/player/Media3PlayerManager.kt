package com.fluxplayer.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.fluxplayer.data.datasource.FluxDataSourceFactory
import com.fluxplayer.ui.player.state.PlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val fluxDataSourceFactory: FluxDataSourceFactory
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    val exoPlayer: ExoPlayer = createExoPlayer()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(fluxDataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(createPlayerListener())
            }
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playerState.update {
                        it.copy(
                            isBuffering = true,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                            bufferedPositionMs = exoPlayer.bufferedPosition
                        )
                    }
                }
                Player.STATE_READY -> {
                    _playerState.update {
                        it.copy(
                            isBuffering = false,
                            isEnded = false,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                            currentPositionMs = exoPlayer.currentPosition,
                            bufferedPositionMs = exoPlayer.bufferedPosition
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _playerState.update { it.copy(isPlaying = false, isEnded = true) }
                    stopProgressTracker()
                }
                Player.STATE_IDLE -> {
                    _playerState.update { it.copy(isBuffering = false) }
                    stopProgressTracker()
                }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _playerState.update {
                it.copy(
                    videoWidth = videoSize.width,
                    videoHeight = videoSize.height
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.update {
                it.copy(
                    errorMessage = error.localizedMessage ?: "Playback error: ${error.errorCodeName}",
                    isBuffering = false
                )
            }
        }
    }

    fun playUri(uri: Uri, title: String) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        _playerState.update {
            it.copy(
                uri = uri,
                title = title,
                errorMessage = null,
                isEnded = false
            )
        }
    }

    fun play() {
        if (_playerState.value.isEnded) {
            exoPlayer.seekTo(0L)
        }
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(newPos)
    }

    fun toggleControls() {
        _playerState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _playerState.update { it.copy(controlsVisible = visible) }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                _playerState.update {
                    it.copy(
                        currentPositionMs = exoPlayer.currentPosition,
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    )
                }
                delay(200) // Update 5 times per second for smooth seekbar
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun close() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.update { PlayerState() }
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.release()
    }
}

