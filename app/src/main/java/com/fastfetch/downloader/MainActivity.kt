package com.fastfetch.downloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.fastfetch.downloader.ui.components.MediaPlayerDialog
import com.fastfetch.downloader.ui.components.PreviewDialog
import com.fastfetch.downloader.ui.screens.DownloadsScreen
import com.fastfetch.downloader.ui.screens.HomeScreen
import com.fastfetch.downloader.ui.screens.SettingsScreen
import com.fastfetch.downloader.ui.theme.FastFetchTheme
import com.fastfetch.downloader.ui.theme.PrimaryCyan
import com.fastfetch.downloader.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val urlInputText by viewModel.urlInputText.collectAsState()
            val isLoadingPreview by viewModel.isLoadingPreview.collectAsState()
            val activePreview by viewModel.activePreview.collectAsState()
            val activePlayerItem by viewModel.activePlayerItem.collectAsState()
            val downloadsHistory by viewModel.downloadsHistory.collectAsState()

            FastFetchTheme(themeMode = appSettings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { viewModel.setTab(0) },
                                    icon = { Icon(Icons.Default.Download, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        indicatorColor = PrimaryCyan
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { viewModel.setTab(1) },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Downloads") },
                                    label = { Text("Downloads") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        indicatorColor = PrimaryCyan
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { viewModel.setTab(2) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        indicatorColor = PrimaryCyan
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(targetState = currentTab, label = "ScreenTransition") { tab ->
                                when (tab) {
                                    0 -> HomeScreen(
                                        urlText = urlInputText,
                                        onUrlChange = viewModel::onUrlInputChanged,
                                        isLoadingPreview = isLoadingPreview,
                                        onDownloadClick = { viewModel.inspectUrl(this@MainActivity) },
                                        recentDownloads = downloadsHistory,
                                        onTabSelected = viewModel::setTab,
                                        onPause = viewModel::pauseDownload,
                                        onResume = viewModel::resumeDownload,
                                        onCancel = viewModel::cancelDownload,
                                        onOpen = viewModel::openInAppPlayer,
                                        onShare = { viewModel.shareFile(this@MainActivity, it) },
                                        onDelete = viewModel::deleteFile
                                    )
                                    1 -> DownloadsScreen(
                                        downloads = downloadsHistory,
                                        onTabSelected = viewModel::setTab,
                                        onPause = viewModel::pauseDownload,
                                        onResume = viewModel::resumeDownload,
                                        onCancel = viewModel::cancelDownload,
                                        onOpen = viewModel::openInAppPlayer,
                                        onShare = { viewModel.shareFile(this@MainActivity, it) },
                                        onDelete = viewModel::deleteFile
                                    )
                                    2 -> SettingsScreen(
                                        settings = appSettings,
                                        onSettingsChanged = viewModel::updateSettings,
                                        onClearHistory = viewModel::clearHistory,
                                        onTabSelected = viewModel::setTab
                                    )
                                }
                            }
                        }
                    }

                    // Render URL Preview dialog when inspecting link
                    activePreview?.let { preview ->
                        PreviewDialog(
                            preview = preview,
                            onConfirmDownload = viewModel::confirmDownload,
                            onDismiss = viewModel::dismissPreview
                        )
                    }

                    // Render In-App Built-in Media Player Dialog
                    activePlayerItem?.let { playerItem ->
                        MediaPlayerDialog(
                            item = playerItem,
                            onDismiss = viewModel::closeInAppPlayer,
                            onExportToDevice = { viewModel.openFile(this@MainActivity, playerItem) }
                        )
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
