package com.fastfetch.downloader.model

data class UrlPreview(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long,
    val domainHost: String,
    val isReachable: Boolean = true,
    val errorMessage: String? = null,
    val selectedQuality: String = "720p"
)
