package com.fastfetch.downloader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fastfetch.downloader.MainActivity
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.model.DownloadStatus

class DownloadNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "fastfetch_download_channel"
        const val CHANNEL_NAME = "FastFetch Downloads"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time file download progress and completion alerts"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(item: DownloadItem) {
        val notificationId = item.id.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.fileName)
            .setContentIntent(pendingIntent)
            .setOngoing(item.status == DownloadStatus.DOWNLOADING)
            .setOnlyAlertOnce(true)

        when (item.status) {
            DownloadStatus.DOWNLOADING -> {
                val percent = (item.progressPercent * 100).toInt()
                val speedText = item.formattedSpeed
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentText("$percent% • $speedText")
                    .setProgress(100, percent, item.totalSize <= 0)
                    .setSubText("${item.formattedDownloaded} / ${item.formattedSize}")
            }
            DownloadStatus.COMPLETED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentText("Download complete (${item.formattedSize})")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
            }
            DownloadStatus.FAILED -> {
                builder.setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentText(item.errorMessage ?: "Download failed")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
            }
            DownloadStatus.PAUSED -> {
                builder.setSmallIcon(android.R.drawable.ic_media_pause)
                    .setContentText("Download paused")
                    .setProgress(100, (item.progressPercent * 100).toInt(), false)
                    .setOngoing(false)
            }
            DownloadStatus.CANCELLED -> {
                notificationManager.cancel(notificationId)
                return
            }
            else -> return
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Notification permission missing on Android 13+
        }
    }

    fun cancelNotification(itemId: String) {
        notificationManager.cancel(itemId.hashCode())
    }
}
