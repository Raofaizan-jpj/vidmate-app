package com.fastfetch.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.model.DownloadStatus
import com.fastfetch.downloader.ui.theme.ErrorRed
import com.fastfetch.downloader.ui.theme.PrimaryCyan
import com.fastfetch.downloader.ui.theme.SuccessGreen
import com.fastfetch.downloader.ui.theme.WarningOrange

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // File Type Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getIconBackground(item.mimeType)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMimeIcon(item.mimeType),
                        contentDescription = "File Type",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status = item.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.mimeType.split("/").lastOrNull()?.uppercase() ?: "FILE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Action buttons right side
                Row {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = WarningOrange
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ErrorRed
                                )
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = PrimaryCyan
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ErrorRed
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onOpen) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open File",
                                    tint = SuccessGreen
                                )
                            }
                            IconButton(onClick = onShare) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share File",
                                    tint = PrimaryCyan
                                )
                            }
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete File",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        else -> {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and details for downloading or paused states
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                LinearProgressIndicator(
                    progress = item.progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryCyan,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(item.progressPercent * 100).toInt()}% • ${item.formattedDownloaded} / ${item.formattedSize}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (item.status == DownloadStatus.DOWNLOADING && item.speedBytesPerSec > 0) {
                        Text(
                            text = item.formattedSpeed,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryCyan
                        )
                    }
                }
            } else if (item.status == DownloadStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved in Downloads (${item.formattedSize})",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen
                    )
                }
            } else if (item.status == DownloadStatus.FAILED) {
                Text(
                    text = item.errorMessage ?: "Download failed. Tap delete or check URL.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (bgColor, textColor, text) = when (status) {
        DownloadStatus.QUEUED -> Triple(Color(0xFF334155), Color.White, "Queued")
        DownloadStatus.DOWNLOADING -> Triple(PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan, "Downloading")
        DownloadStatus.PAUSED -> Triple(WarningOrange.copy(alpha = 0.2f), WarningOrange, "Paused")
        DownloadStatus.COMPLETED -> Triple(SuccessGreen.copy(alpha = 0.2f), SuccessGreen, "Completed")
        DownloadStatus.FAILED -> Triple(ErrorRed.copy(alpha = 0.2f), ErrorRed, "Failed")
        DownloadStatus.CANCELLED -> Triple(Color.Gray.copy(alpha = 0.2f), Color.Gray, "Cancelled")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getMimeIcon(mimeType: String): ImageVector {
    val lower = mimeType.lowercase()
    return when {
        lower.startsWith("video/") -> Icons.Default.VideoFile
        lower.startsWith("audio/") -> Icons.Default.AudioFile
        lower.startsWith("image/") -> Icons.Default.Image
        lower.contains("pdf") || lower.contains("document") || lower.contains("text/") -> Icons.Default.Description
        lower.contains("zip") || lower.contains("rar") || lower.contains("tar") || lower.contains("compressed") -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getIconBackground(mimeType: String): Color {
    val lower = mimeType.lowercase()
    return when {
        lower.startsWith("video/") -> Color(0xFFE11D48)
        lower.startsWith("audio/") -> Color(0xFF8B5CF6)
        lower.startsWith("image/") -> Color(0xFF10B981)
        lower.contains("pdf") || lower.contains("document") -> Color(0xFF0284C7)
        lower.contains("zip") || lower.contains("compressed") -> Color(0xFFF59E0B)
        else -> Color(0xFF64748B)
    }
}
