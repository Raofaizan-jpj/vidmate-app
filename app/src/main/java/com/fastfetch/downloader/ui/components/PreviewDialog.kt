package com.fastfetch.downloader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.model.UrlPreview
import com.fastfetch.downloader.ui.theme.PrimaryCyan

@Composable
fun PreviewDialog(
    preview: UrlPreview,
    onConfirmDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedQuality by remember { mutableStateOf(preview.selectedQuality) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "File Information Preview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!preview.isReachable) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = preview.errorMessage ?: "The server could not be reached.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                PreviewRow(
                    icon = Icons.Default.Description,
                    label = "File Name",
                    value = preview.fileName
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreviewRow(
                    icon = Icons.Default.Info,
                    label = "File Type",
                    value = preview.mimeType
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreviewRow(
                    icon = Icons.Default.Storage,
                    label = "Estimated Size",
                    value = DownloadItem.formatBytes(preview.contentLength)
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreviewRow(
                    icon = Icons.Default.Dns,
                    label = "Domain Host",
                    value = preview.domainHost
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Format / HD Quality Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = "HD Quality",
                        tint = PrimaryCyan,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Format / HD Quality",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QualityChip(
                            label = "1080p Full HD",
                            selected = selectedQuality == "1080p",
                            onSelect = { selectedQuality = "1080p" },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        QualityChip(
                            label = "720p HD",
                            selected = selectedQuality == "720p",
                            onSelect = { selectedQuality = "720p" }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        QualityChip(
                            label = "480p SD",
                            selected = selectedQuality == "480p",
                            onSelect = { selectedQuality = "480p" },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        QualityChip(
                            label = "360p",
                            selected = selectedQuality == "360p",
                            onSelect = { selectedQuality = "360p" },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        QualityChip(
                            label = "MP3 Audio",
                            selected = selectedQuality == "mp3",
                            onSelect = { selectedQuality = "mp3" }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDownload(selectedQuality) },
                enabled = preview.isReachable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Start Download", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryCyan,
            selectedLabelColor = Color.Black
        ),
        modifier = modifier
    )
}

@Composable
private fun PreviewRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryCyan,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}
