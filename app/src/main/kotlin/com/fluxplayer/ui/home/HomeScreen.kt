package com.fluxplayer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxplayer.ui.home.components.NetworkStorageTab
import com.fluxplayer.ui.player.PlayerViewModel
import com.fluxplayer.ui.theme.PrimaryTeal
import com.fluxplayer.ui.theme.SurfaceDark

@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onVideoSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val networkState by viewModel.networkState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onVideoSelected(it) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SurfaceDark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF14171E),
                contentColor = PrimaryTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryTeal
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Local Files", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Network Storage", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Lan, contentDescription = null) }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = "FluxPlayer Logo",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(96.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "FluxPlayer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Next-Generation Android Video Player\nClean-Room Media3 + Jetpack Compose Architecture",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Open Video via SAF",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                1 -> {
                    NetworkStorageTab(
                        networkState = networkState,
                        onProtocolSelected = { viewModel.selectProtocol(it) },
                        onSmbHostChange = { viewModel.updateSmbHost(it) },
                        onSmbShareChange = { viewModel.updateSmbShare(it) },
                        onSmbUsernameChange = { viewModel.updateSmbUsername(it) },
                        onSmbPasswordChange = { viewModel.updateSmbPassword(it) },
                        onSmbDomainChange = { viewModel.updateSmbDomain(it) },
                        onWebDavUrlChange = { viewModel.updateWebDavUrl(it) },
                        onWebDavUsernameChange = { viewModel.updateWebDavUsername(it) },
                        onWebDavPasswordChange = { viewModel.updateWebDavPassword(it) },
                        onConnectClick = { viewModel.connectNetwork() },
                        onDisconnectClick = { viewModel.disconnectNetwork() },
                        onDirectoryClick = { viewModel.openDirectory(it) },
                        onNavigateBackClick = { viewModel.navigateBackDirectory() },
                        onVideoClick = { viewModel.playNetworkItem(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
