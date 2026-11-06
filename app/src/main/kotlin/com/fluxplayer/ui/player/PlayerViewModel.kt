package com.fluxplayer.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxplayer.player.Media3PlayerManager
import com.fluxplayer.ui.player.state.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: Media3PlayerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val exoPlayer = playerManager.exoPlayer

    fun loadMedia(uri: Uri) {
        viewModelScope.launch {
            // Take persistable permission if content URI
            if (uri.scheme == "content") {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: Exception) {
                    // Non-persistable URI (e.g. temporary grant from third-party app)
                }
            }

            val title = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Unknown Video"
            playerManager.playUri(uri, title)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = playerManager.seekBy(deltaMs)

    fun toggleControls() = playerManager.toggleControls()

    fun hideControls() = playerManager.setControlsVisible(false)

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

