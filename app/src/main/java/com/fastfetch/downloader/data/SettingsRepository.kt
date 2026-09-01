package com.fastfetch.downloader.data

import android.content.Context
import android.content.SharedPreferences
import com.fastfetch.downloader.model.AppSettings
import com.fastfetch.downloader.model.AppThemeMode
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fastfetch_settings_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val PREF_KEY = "app_settings"

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val json = prefs.getString(PREF_KEY, null) ?: return AppSettings()
        return try {
            gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        try {
            val json = gson.toJson(newSettings)
            prefs.edit().putString(PREF_KEY, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
