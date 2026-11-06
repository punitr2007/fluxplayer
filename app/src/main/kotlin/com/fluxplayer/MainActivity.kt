package com.fluxplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fluxplayer.ui.home.HomeScreen
import com.fluxplayer.ui.player.PlayerScreen
import com.fluxplayer.ui.player.PlayerViewModel
import com.fluxplayer.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val playerViewModel: PlayerViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { playerViewModel.loadMedia(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        observePlayerStateForPerformance()

        setContent {
            FluxTheme {
                val playerState by playerViewModel.playerState.collectAsState()

                if (playerState.uri != null) {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onBackClick = {
                            playerViewModel.closePlayer()
                        },
                        onOpenFileClick = {
                            openSafPicker()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    HomeScreen(
                        viewModel = playerViewModel,
                        onVideoSelected = { uri ->
                            playerViewModel.loadMedia(uri)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        setSustainedPerformance(false)
    }

    private fun observePlayerStateForPerformance() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.playerState.collect { state ->
                    val isPlaying = state.uri != null && state.isPlaying
                    val isPlayerOpen = state.uri != null
                    updatePerformanceAndDisplayMode(isPlaying, isPlayerOpen)
                }
            }
        }
    }

    private fun updatePerformanceAndDisplayMode(isPlaying: Boolean, isPlayerOpen: Boolean) {
        // 1. Auto Frame Rate (AFR): match display's highest refresh rate
        if (isPlayerOpen) {
            applyAutoFrameRate()
        } else {
            resetDisplayMode()
        }

        // 2. Sustained Performance Mode: prevent Snapdragon thermal throttling during FRC rendering
        setSustainedPerformance(isPlaying)
    }

    private fun applyAutoFrameRate() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: windowManager.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val modes = display?.supportedModes ?: return
        val maxRefreshMode = modes.maxByOrNull { it.refreshRate }
        maxRefreshMode?.let {
            val params = window.attributes
            if (params.preferredDisplayModeId != it.modeId) {
                Log.i(TAG, "AFR: Requesting display mode ${it.modeId} (${it.refreshRate}Hz ${it.physicalWidth}x${it.physicalHeight})")
                params.preferredDisplayModeId = it.modeId
                window.attributes = params
            }
        }
    }

    private fun resetDisplayMode() {
        val params = window.attributes
        if (params.preferredDisplayModeId != 0) {
            Log.i(TAG, "Resetting display mode to default (0)")
            params.preferredDisplayModeId = 0
            window.attributes = params
        }
    }

    private fun setSustainedPerformance(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isSupported = powerManager?.isSustainedPerformanceModeSupported == true
            Log.d(TAG, "Sustained performance mode: enable=$enable, supported=$isSupported")
            try {
                window.setSustainedPerformanceMode(enable)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to set sustained performance mode: ${e.message}")
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                playerViewModel.loadMedia(uri)
            }
        }
    }

    private fun openSafPicker() {
        filePickerLauncher.launch(arrayOf("video/*"))
    }
}
