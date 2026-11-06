package com.fluxplayer.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxplayer.data.model.NetworkItem
import com.fluxplayer.ui.home.state.NetworkProtocol
import com.fluxplayer.ui.home.state.NetworkUiState
import com.fluxplayer.ui.theme.PrimaryTeal
import com.fluxplayer.ui.theme.SurfaceDark

@Composable
fun NetworkStorageTab(
    networkState: NetworkUiState,
    onProtocolSelected: (NetworkProtocol) -> Unit,
    onSmbHostChange: (String) -> Unit,
    onSmbShareChange: (String) -> Unit,
    onSmbUsernameChange: (String) -> Unit,
    onSmbPasswordChange: (String) -> Unit,
    onSmbDomainChange: (String) -> Unit,
    onWebDavUrlChange: (String) -> Unit,
    onWebDavUsernameChange: (String) -> Unit,
    onWebDavPasswordChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onDirectoryClick: (NetworkItem) -> Unit,
    onNavigateBackClick: () -> Unit,
    onVideoClick: (NetworkItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (networkState.isConnected) {
        ConnectedBrowser(
            networkState = networkState,
            onDirectoryClick = onDirectoryClick,
            onNavigateBackClick = onNavigateBackClick,
            onVideoClick = onVideoClick,
            onDisconnectClick = onDisconnectClick,
            modifier = modifier
        )
    } else {
        ConnectionForm(
            networkState = networkState,
            onProtocolSelected = onProtocolSelected,
            onSmbHostChange = onSmbHostChange,
            onSmbShareChange = onSmbShareChange,
            onSmbUsernameChange = onSmbUsernameChange,
            onSmbPasswordChange = onSmbPasswordChange,
            onSmbDomainChange = onSmbDomainChange,
            onWebDavUrlChange = onWebDavUrlChange,
            onWebDavUsernameChange = onWebDavUsernameChange,
            onWebDavPasswordChange = onWebDavPasswordChange,
            onConnectClick = onConnectClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ConnectionForm(
    networkState: NetworkUiState,
    onProtocolSelected: (NetworkProtocol) -> Unit,
    onSmbHostChange: (String) -> Unit,
    onSmbShareChange: (String) -> Unit,
    onSmbUsernameChange: (String) -> Unit,
    onSmbPasswordChange: (String) -> Unit,
    onSmbDomainChange: (String) -> Unit,
    onWebDavUrlChange: (String) -> Unit,
    onWebDavUsernameChange: (String) -> Unit,
    onWebDavPasswordChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect to Network Storage",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Stream video directly from your NAS, PC, or Cloud",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Protocol Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = networkState.selectedProtocol == NetworkProtocol.SMB,
                onClick = { onProtocolSelected(NetworkProtocol.SMB) },
                label = { Text("SMB (Windows / NAS)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryTeal,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            FilterChip(
                selected = networkState.selectedProtocol == NetworkProtocol.WEBDAV,
                onClick = { onProtocolSelected(NetworkProtocol.WEBDAV) },
                label = { Text("WebDAV") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryTeal,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (networkState.selectedProtocol == NetworkProtocol.SMB) {
                    OutlinedTextField(
                        value = networkState.smbConfig.host,
                        onValueChange = onSmbHostChange,
                        label = { Text("Server IP / Hostname") },
                        placeholder = { Text("e.g. 192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.smbConfig.share,
                        onValueChange = onSmbShareChange,
                        label = { Text("Share Name") },
                        placeholder = { Text("e.g. media or movies") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.smbConfig.username,
                        onValueChange = onSmbUsernameChange,
                        label = { Text("Username (optional)") },
                        placeholder = { Text("Leave blank for guest") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.smbConfig.password,
                        onValueChange = onSmbPasswordChange,
                        label = { Text("Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.smbConfig.domain,
                        onValueChange = onSmbDomainChange,
                        label = { Text("Workgroup / Domain (optional)") },
                        placeholder = { Text("WORKGROUP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )
                } else {
                    OutlinedTextField(
                        value = networkState.webDavConfig.serverUrl,
                        onValueChange = onWebDavUrlChange,
                        label = { Text("WebDAV Server URL") },
                        placeholder = { Text("http://192.168.1.100:8080/webdav") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.webDavConfig.username,
                        onValueChange = onWebDavUsernameChange,
                        label = { Text("Username (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = networkState.webDavConfig.password,
                        onValueChange = onWebDavPasswordChange,
                        label = { Text("Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )
                }
            }
        }

        if (networkState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1A1A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Connection Error: ${networkState.errorMessage}",
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onConnectClick()
            },
            enabled = !networkState.isConnecting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (networkState.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Connect & Browse",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConnectedBrowser(
    networkState: NetworkUiState,
    onDirectoryClick: (NetworkItem) -> Unit,
    onNavigateBackClick: () -> Unit,
    onVideoClick: (NetworkItem) -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (networkState.currentPath.isNotEmpty()) {
                IconButton(onClick = onNavigateBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (networkState.selectedProtocol == NetworkProtocol.SMB) "SMB Share" else "WebDAV Server",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal
                )
                Text(
                    text = "/" + networkState.currentPath,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onDisconnectClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Disconnect", color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        if (networkState.isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryTeal)
            }
        } else if (networkState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files or folders found in this directory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(networkState.items) { item ->
                    NetworkItemRow(
                        item = item,
                        onClick = {
                            if (item.isDirectory) {
                                onDirectoryClick(item)
                            } else if (item.isVideo) {
                                onVideoClick(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkItemRow(
    item: NetworkItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            item.isDirectory -> Icons.Default.Folder
            item.isVideo -> Icons.Default.VideoFile
            else -> Icons.Default.InsertDriveFile
        }

        val tint = when {
            item.isDirectory -> Color(0xFFFFD54F) // Folder yellow
            item.isVideo -> PrimaryTeal
            else -> Color.Gray
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (item.isDirectory || item.isVideo) Color.White else Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        if (item.isDirectory) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open folder",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        } else if (item.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play video",
                tint = PrimaryTeal,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryTeal,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = PrimaryTeal,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

