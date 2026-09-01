package com.fastfetch.downloader.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fastfetch.downloader.data.HistoryRepository
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.model.DownloadStatus
import com.fastfetch.downloader.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadEngine(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val notificationManager = DownloadNotificationManager(context)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, Boolean>()

    fun startDownload(item: DownloadItem) {
        pauseFlags[item.id] = false
        val job = scope.launch {
            runDownloadJob(item)
        }
        activeJobs[item.id] = job
    }

    fun pauseDownload(id: String) {
        pauseFlags[id] = true
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        val currentList = historyRepository.items.value
        val item = currentList.find { it.id == id } ?: return
        val updated = item.copy(
            status = DownloadStatus.PAUSED,
            speedBytesPerSec = 0L
        )
        historyRepository.upsertItem(updated)
        notificationManager.updateNotification(updated)
    }

    fun resumeDownload(id: String) {
        val currentList = historyRepository.items.value
        val item = currentList.find { it.id == id } ?: return
        startDownload(item)
    }

    fun cancelDownload(id: String) {
        pauseFlags[id] = false
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        val currentList = historyRepository.items.value
        val item = currentList.find { it.id == id } ?: return
        val updated = item.copy(
            status = DownloadStatus.CANCELLED,
            speedBytesPerSec = 0L
        )
        historyRepository.upsertItem(updated)
        notificationManager.cancelNotification(id)
    }

    fun openFile(context: Context, item: DownloadItem) {
        val uriStr = item.localUriString ?: return
        try {
            val uri = Uri.parse(uriStr)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open File With"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareFile(context: Context, item: DownloadItem) {
        val uriStr = item.localUriString ?: return
        try {
            val uri = Uri.parse(uriStr)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share File"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFile(item: DownloadItem) {
        cancelDownload(item.id)
        item.localUriString?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                when (uri.scheme) {
                    "content" -> context.contentResolver.delete(uri, null, null)
                    "file" -> uri.path?.let { File(it).delete() }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        historyRepository.removeItem(item.id)
    }

    private suspend fun runDownloadJob(initialItem: DownloadItem) {
        var item = initialItem.copy(
            status = DownloadStatus.DOWNLOADING,
            errorMessage = null
        )
        historyRepository.upsertItem(item)
        notificationManager.updateNotification(item)

        if (!NetworkUtils.isNetworkAvailable(context)) {
            val failed = item.copy(
                status = DownloadStatus.FAILED,
                errorMessage = "No internet connection. Check connection and retry."
            )
            historyRepository.upsertItem(failed)
            notificationManager.updateNotification(failed)
            return
        }

        var outputStream: OutputStream? = null
        var fileUri: Uri? = null
        var tempFile: File? = null

        try {
            val existingDownloaded = item.downloadedBytes
            val requestBuilder = Request.Builder()
                .url(item.url)
                .header("User-Agent", "FastFetchDownloader/1.0")

            if (existingDownloaded > 0 && item.totalSize > 0) {
                requestBuilder.header("Range", "bytes=$existingDownloaded-")
            }

            val response = NetworkUtils.okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                val failed = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "HTTP Server Error (${response.code})"
                )
                historyRepository.upsertItem(failed)
                notificationManager.updateNotification(failed)
                response.close()
                return
            }

            val responseBody = response.body ?: run {
                val failed = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "Empty response body from server"
                )
                historyRepository.upsertItem(failed)
                notificationManager.updateNotification(failed)
                return
            }

            val totalSize = if (response.code == 206) {
                item.totalSize
            } else {
                responseBody.contentLength().let { if (it > 0) it else item.totalSize }
            }

            item = item.copy(totalSize = totalSize)

            // Setup Storage Stream using MediaStore for Android Q+ or FileProvider
            val isAppend = (response.code == 206 && existingDownloaded > 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (fileUri == null) {
                    throw IllegalStateException("Failed to create MediaStore record")
                }
                outputStream = resolver.openOutputStream(fileUri, if (isAppend) "wa" else "w")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, item.fileName)
                outputStream = FileOutputStream(targetFile, isAppend)
                fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            }

            val inputStream: InputStream = responseBody.byteStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalDownloaded = if (isAppend) existingDownloaded else 0L

            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (pauseFlags[item.id] == true) {
                    outputStream?.flush()
                    outputStream?.close()
                    response.close()
                    return
                }

                outputStream?.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                bytesSinceLastSample += bytesRead

                val now = System.currentTimeMillis()
                val timeDiff = now - lastTime
                if (timeDiff >= 600) {
                    val speed = (bytesSinceLastSample * 1000) / timeDiff
                    item = item.copy(
                        downloadedBytes = totalDownloaded,
                        speedBytesPerSec = speed,
                        localUriString = fileUri?.toString()
                    )
                    historyRepository.upsertItem(item)
                    notificationManager.updateNotification(item)

                    lastTime = now
                    bytesSinceLastSample = 0L
                }
            }

            outputStream?.flush()
            outputStream?.close()
            response.close()

            val completed = item.copy(
                downloadedBytes = totalDownloaded,
                totalSize = if (totalSize > 0) totalSize else totalDownloaded,
                speedBytesPerSec = 0L,
                localUriString = fileUri?.toString(),
                status = DownloadStatus.COMPLETED
            )
            historyRepository.upsertItem(completed)
            notificationManager.updateNotification(completed)

        } catch (e: Exception) {
            e.printStackTrace()
            outputStream?.close()

            if (pauseFlags[item.id] == true) {
                val paused = item.copy(
                    status = DownloadStatus.PAUSED,
                    speedBytesPerSec = 0L
                )
                historyRepository.upsertItem(paused)
                notificationManager.updateNotification(paused)
            } else {
                val failed = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Network read error",
                    speedBytesPerSec = 0L
                )
                historyRepository.upsertItem(failed)
                notificationManager.updateNotification(failed)
            }
        } finally {
            activeJobs.remove(item.id)
            pauseFlags.remove(item.id)
        }
    }
}
