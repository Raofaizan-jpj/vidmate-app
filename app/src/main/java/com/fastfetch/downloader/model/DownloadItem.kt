package com.fastfetch.downloader.model

import java.util.UUID

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val totalSize: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val mimeType: String = "*/*",
    val localUriString: String? = null,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val progressPercent: Float
        get() {
            if (totalSize <= 0) return 0f
            return (downloadedBytes.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
        }

    val formattedSize: String
        get() = formatBytes(totalSize)

    val formattedDownloaded: String
        get() = formatBytes(downloadedBytes)

    val formattedSpeed: String
        get() = if (speedBytesPerSec > 0) "${formatBytes(speedBytesPerSec)}/s" else "0 B/s"

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "Size unavailable"
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}
