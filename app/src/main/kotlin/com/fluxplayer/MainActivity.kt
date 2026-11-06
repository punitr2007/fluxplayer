package com.fluxplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.fluxplayer.ui.home.HomeScreen
import com.fluxplayer.ui.player.PlayerScreen
import com.fluxplayer.ui.player.PlayerViewModel
import com.fluxplayer.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { playerViewModel.loadMedia(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle incoming video intent (from file manager, browser, or gallery)
        handleIntent(intent)

        setContent {
            FluxTheme {
                val playerState by playerViewModel.playerState.collectAsState()

                if (playerState.uri != null) {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onBackClick = {
                            playerViewModel.togglePlayPause()
                        },
                        onOpenFileClick = {
                            openSafPicker()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    HomeScreen(
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

