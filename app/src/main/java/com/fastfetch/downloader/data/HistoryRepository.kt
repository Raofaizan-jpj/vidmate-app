package com.fastfetch.downloader.data

import android.content.Context
import android.content.SharedPreferences
import com.fastfetch.downloader.model.DownloadItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fastfetch_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val PREF_KEY = "download_history_items"

    private val _items = MutableStateFlow<List<DownloadItem>>(loadHistory())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private fun loadHistory(): List<DownloadItem> {
        val json = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadItem>>() {}.type
            gson.fromJson<List<DownloadItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<DownloadItem>) {
        try {
            val json = gson.toJson(list)
            prefs.edit().putString(PREF_KEY, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun upsertItem(item: DownloadItem) {
        val currentList = _items.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            currentList[index] = item
        } else {
            currentList.add(0, item)
        }
        _items.value = currentList
        saveHistory(currentList)
    }

    fun removeItem(id: String) {
        val currentList = _items.value.filterNot { it.id == id }
        _items.value = currentList
        saveHistory(currentList)
    }

    fun clearAll() {
        _items.value = emptyList()
        prefs.edit().remove(PREF_KEY).apply()
    }
}
