package com.fastfetch.downloader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.ui.components.AppHeader
import com.fastfetch.downloader.ui.components.DownloadItemCard
import com.fastfetch.downloader.ui.components.UrlInputCard
import com.fastfetch.downloader.ui.theme.PrimaryCyan

@Composable
fun HomeScreen(
    urlText: String,
    onUrlChange: (String) -> Unit,
    isLoadingPreview: Boolean,
    onDownloadClick: () -> Unit,
    recentDownloads: List<DownloadItem>,
    onTabSelected: (Int) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onDelete: (DownloadItem) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(currentTab = 0, onTabSelected = onTabSelected)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                UrlInputCard(
                    urlText = urlText,
                    onUrlChange = onUrlChange,
                    isLoadingPreview = isLoadingPreview,
                    onDownloadClick = onDownloadClick
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Downloads",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (recentDownloads.isNotEmpty()) {
                        TextButton(onClick = { onTabSelected(1) }) {
                            Text(text = "View All", color = PrimaryCyan)
                        }
                    }
                }
            }

            if (recentDownloads.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No downloads yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste a direct link above to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(recentDownloads.take(5), key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onCancel = { onCancel(item.id) },
                        onOpen = { onOpen(item) },
                        onShare = { onShare(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}
