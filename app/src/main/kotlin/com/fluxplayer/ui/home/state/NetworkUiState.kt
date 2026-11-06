package com.fluxplayer.ui.home.state

import com.fluxplayer.data.model.NetworkItem
import com.fluxplayer.data.model.SmbConfig
import com.fluxplayer.data.model.WebDavConfig

enum class NetworkProtocol {
    SMB,
    WEBDAV
}

data class NetworkUiState(
    val selectedProtocol: NetworkProtocol = NetworkProtocol.SMB,
    val smbConfig: SmbConfig = SmbConfig(host = "192.168.1.100", share = "shared"),
    val webDavConfig: WebDavConfig = WebDavConfig(serverUrl = "http://192.168.1.100:8080/webdav"),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val currentPath: String = "",
    val items: List<NetworkItem> = emptyList()
)

