package com.fastfetch.downloader.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fastfetch.downloader.data.HistoryRepository
import com.fastfetch.downloader.data.SettingsRepository
import com.fastfetch.downloader.download.DownloadEngine
import com.fastfetch.downloader.model.AppSettings
import com.fastfetch.downloader.model.DownloadItem
import com.fastfetch.downloader.model.UrlPreview
import com.fastfetch.downloader.network.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val historyRepository = HistoryRepository(application)
    val settingsRepository = SettingsRepository(application)
    val downloadEngine = DownloadEngine(application, historyRepository)

    val downloadsHistory: StateFlow<List<DownloadItem>> = historyRepository.items
    val appSettings: StateFlow<AppSettings> = settingsRepository.settings

    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: History, 2: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _urlInputText = MutableStateFlow("")
    val urlInputText: StateFlow<String> = _urlInputText.asStateFlow()

    private val _isLoadingPreview = MutableStateFlow(false)
    val isLoadingPreview: StateFlow<Boolean> = _isLoadingPreview.asStateFlow()

    private val _activePreview = MutableStateFlow<UrlPreview?>(null)
    val activePreview: StateFlow<UrlPreview?> = _activePreview.asStateFlow()

    private val _activePlayerItem = MutableStateFlow<DownloadItem?>(null)
    val activePlayerItem: StateFlow<DownloadItem?> = _activePlayerItem.asStateFlow()

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun onUrlInputChanged(newUrl: String) {
        _urlInputText.value = newUrl
    }

    fun inspectUrl(context: Context) {
        val url = _urlInputText.value.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            _isLoadingPreview.value = true
            val preview = NetworkUtils.previewUrl(context, url)
            _activePreview.value = preview
            _isLoadingPreview.value = false
        }
    }

    fun dismissPreview() {
        _activePreview.value = null
    }

    fun confirmDownload(selectedQuality: String = "720p") {
        val preview = _activePreview.value ?: return
        
        var name = preview.fileName
        var mime = preview.mimeType
        var size = preview.contentLength

        val baseName = if (name.contains(".")) name.substringBeforeLast(".") else name
        when (selectedQuality) {
            "mp3" -> {
                name = "${baseName}.mp3"
                mime = "audio/mpeg"
                if (size > 0) size = (size * 0.25).toLong()
            }
            "1080p" -> {
                name = "${baseName}_1080p.mp4"
                mime = "video/mp4"
                if (size > 0) size = (size * 1.8).toLong()
            }
            "480p" -> {
                name = "${baseName}_480p.mp4"
                mime = "video/mp4"
                if (size > 0) size = (size * 0.6).toLong()
            }
            "360p" -> {
                name = "${baseName}_360p.mp4"
                mime = "video/mp4"
                if (size > 0) size = (size * 0.4).toLong()
            }
            else -> {
                name = "${baseName}_720p.mp4"
                mime = "video/mp4"
            }
        }

        val newItem = DownloadItem(
            fileName = name,
            url = preview.url,
            totalSize = size,
            mimeType = mime
        )
        _activePreview.value = null
        _urlInputText.value = ""
        downloadEngine.startDownload(newItem)
        _currentTab.value = 1 // Switch to Downloads tab to see progress
    }

    fun openInAppPlayer(item: DownloadItem) {
        _activePlayerItem.value = item
    }

    fun closeInAppPlayer() {
        _activePlayerItem.value = null
    }

    fun pauseDownload(id: String) {
        downloadEngine.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadEngine.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadEngine.cancelDownload(id)
    }

    fun openFile(context: Context, item: DownloadItem) {
        downloadEngine.openFile(context, item)
    }

    fun shareFile(context: Context, item: DownloadItem) {
        downloadEngine.shareFile(context, item)
    }

    fun deleteFile(item: DownloadItem) {
        downloadEngine.deleteFile(item)
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.updateSettings(newSettings)
    }

    fun clearHistory() {
        historyRepository.clearAll()
    }
}
