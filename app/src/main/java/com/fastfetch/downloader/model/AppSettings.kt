package com.fastfetch.downloader.model

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppSettings(
    val downloadLocation: String = "Internal Downloads (MediaStore)",
    val wifiOnly: Boolean = false,
    val autoStart: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.DARK
)
