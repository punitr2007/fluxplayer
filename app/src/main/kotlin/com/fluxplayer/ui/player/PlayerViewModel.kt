package com.fluxplayer.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxplayer.data.model.NetworkItem
import com.fluxplayer.data.repository.NetworkRepository
import com.fluxplayer.player.Media3PlayerManager
import com.fluxplayer.ui.home.state.NetworkProtocol
import com.fluxplayer.ui.home.state.NetworkUiState
import com.fluxplayer.ui.player.state.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: Media3PlayerManager,
    private val networkRepository: NetworkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val exoPlayer = playerManager.exoPlayer

    private val _networkState = MutableStateFlow(NetworkUiState())
    val networkState: StateFlow<NetworkUiState> = _networkState.asStateFlow()

    fun loadMedia(uri: Uri) {
        viewModelScope.launch {
            if (uri.scheme == "content") {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: Exception) {
                    // Non-persistable URI
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

    fun closePlayer() = playerManager.close()

    fun setFrcEnabled(enabled: Boolean) = playerManager.setFrcEnabled(enabled)

    fun setFrcBlendWeight(weight: Float) = playerManager.setFrcBlendWeight(weight)

    // --- Network Management ---

    fun selectProtocol(protocol: NetworkProtocol) {
        _networkState.update { it.copy(selectedProtocol = protocol, errorMessage = null) }
    }

    fun updateSmbHost(host: String) {
        _networkState.update { it.copy(smbConfig = it.smbConfig.copy(host = host)) }
    }

    fun updateSmbShare(share: String) {
        _networkState.update { it.copy(smbConfig = it.smbConfig.copy(share = share)) }
    }

    fun updateSmbUsername(username: String) {
        _networkState.update { it.copy(smbConfig = it.smbConfig.copy(username = username)) }
    }

    fun updateSmbPassword(password: String) {
        _networkState.update { it.copy(smbConfig = it.smbConfig.copy(password = password)) }
    }

    fun updateSmbDomain(domain: String) {
        _networkState.update { it.copy(smbConfig = it.smbConfig.copy(domain = domain)) }
    }

    fun updateWebDavUrl(url: String) {
        _networkState.update { it.copy(webDavConfig = it.webDavConfig.copy(serverUrl = url)) }
    }

    fun updateWebDavUsername(username: String) {
        _networkState.update { it.copy(webDavConfig = it.webDavConfig.copy(username = username)) }
    }

    fun updateWebDavPassword(password: String) {
        _networkState.update { it.copy(webDavConfig = it.webDavConfig.copy(password = password)) }
    }

    fun connectNetwork() {
        viewModelScope.launch {
            val state = _networkState.value
            _networkState.update { it.copy(isConnecting = true, errorMessage = null) }

            when (state.selectedProtocol) {
                NetworkProtocol.SMB -> {
                    val config = state.smbConfig
                    val testResult = networkRepository.testSmbConnection(config)
                    if (testResult.isSuccess) {
                        playerManager.fluxDataSourceFactory.updateSmbCredentials(
                            config.username,
                            config.password,
                            config.domain
                        )
                        loadSmbFiles(subPath = "")
                    } else {
                        _networkState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = testResult.exceptionOrNull()?.localizedMessage ?: "SMB Connection failed"
                            )
                        }
                    }
                }
                NetworkProtocol.WEBDAV -> {
                    val config = state.webDavConfig
                    val testResult = networkRepository.testWebDavConnection(config)
                    if (testResult.isSuccess) {
                        playerManager.fluxDataSourceFactory.updateWebDavCredentials(
                            config.username,
                            config.password
                        )
                        loadWebDavFiles(subPath = "")
                    } else {
                        _networkState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = testResult.exceptionOrNull()?.localizedMessage ?: "WebDAV Connection failed"
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadSmbFiles(subPath: String) {
        val config = _networkState.value.smbConfig
        val result = networkRepository.listSmbFiles(config, subPath)
        if (result.isSuccess) {
            _networkState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = true,
                    currentPath = subPath,
                    items = result.getOrDefault(emptyList()),
                    errorMessage = null
                )
            }
        } else {
            _networkState.update {
                it.copy(
                    isConnecting = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to list SMB directory"
                )
            }
        }
    }

    private suspend fun loadWebDavFiles(subPath: String) {
        val config = _networkState.value.webDavConfig
        val result = networkRepository.listWebDavFiles(config, subPath)
        if (result.isSuccess) {
            _networkState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = true,
                    currentPath = subPath,
                    items = result.getOrDefault(emptyList()),
                    errorMessage = null
                )
            }
        } else {
            _networkState.update {
                it.copy(
                    isConnecting = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to list WebDAV directory"
                )
            }
        }
    }

    fun openDirectory(item: NetworkItem) {
        if (!item.isDirectory) return
        viewModelScope.launch {
            _networkState.update { it.copy(isConnecting = true, errorMessage = null) }
            when (_networkState.value.selectedProtocol) {
                NetworkProtocol.SMB -> loadSmbFiles(item.path)
                NetworkProtocol.WEBDAV -> loadWebDavFiles(item.path)
            }
        }
    }

    fun navigateBackDirectory() {
        val current = _networkState.value.currentPath
        if (current.isEmpty()) return
        val parent = if (current.contains('/')) current.substringBeforeLast('/') else ""
        viewModelScope.launch {
            _networkState.update { it.copy(isConnecting = true, errorMessage = null) }
            when (_networkState.value.selectedProtocol) {
                NetworkProtocol.SMB -> loadSmbFiles(parent)
                NetworkProtocol.WEBDAV -> loadWebDavFiles(parent)
            }
        }
    }

    fun playNetworkItem(item: NetworkItem) {
        val state = _networkState.value
        viewModelScope.launch {
            when (state.selectedProtocol) {
                NetworkProtocol.SMB -> {
                    playerManager.fluxDataSourceFactory.updateSmbCredentials(
                        state.smbConfig.username,
                        state.smbConfig.password,
                        state.smbConfig.domain
                    )
                }
                NetworkProtocol.WEBDAV -> {
                    playerManager.fluxDataSourceFactory.updateWebDavCredentials(
                        state.webDavConfig.username,
                        state.webDavConfig.password
                    )
                }
            }
            playerManager.playUri(item.uri, item.name)
        }
    }

    fun disconnectNetwork() {
        _networkState.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                currentPath = "",
                items = emptyList(),
                errorMessage = null
            )
        }
    }

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
