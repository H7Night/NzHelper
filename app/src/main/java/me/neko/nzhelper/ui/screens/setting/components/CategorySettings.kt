package me.neko.nzhelper.ui.screens.setting.components

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

object CategorySettings {
    private const val PREFS_NAME = "category_prefs"
    private const val KEY_LOCATIONS = "locations_list"
    private const val KEY_PROPS = "props_list"
    private const val KEY_MOODS = "moods_list"

    val DEFAULT_LOCATIONS = listOf("卧室", "沙发", "厕所")
    val DEFAULT_PROPS = listOf("手", "飞机杯", "小胶妻")
    val DEFAULT_MOODS = listOf("平静", "愉悦", "兴奋", "疲惫")

    private fun getList(context: Context, key: String, default: List<String>): List<String> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        return if (json.isNullOrEmpty()) {
            default
        } else {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (_: Exception) {
                default
            }
        }
    }

    private fun saveList(context: Context, key: String, list: List<String>) {
        val json = JSONArray(list).toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(key, json) }
    }

    private fun addItem(context: Context, key: String, default: List<String>, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        val current = getList(context, key, default).toMutableList()
        if (trimmed !in current) {
            current.add(trimmed)
            saveList(context, key, current)
        }
    }

    private fun removeItem(context: Context, key: String, default: List<String>, value: String) {
        val current = getList(context, key, default).toMutableList()
        // 至少保留一项
        if (current.size <= 1) return
        current.remove(value)
        saveList(context, key, current)
    }

    fun getLocations(context: Context): List<String> =
        getList(context, KEY_LOCATIONS, DEFAULT_LOCATIONS)

    fun addLocation(context: Context, value: String) =
        addItem(context, KEY_LOCATIONS, DEFAULT_LOCATIONS, value)

    fun removeLocation(context: Context, value: String) =
        removeItem(context, KEY_LOCATIONS, DEFAULT_LOCATIONS, value)

    fun resetLocations(context: Context) = saveList(context, KEY_LOCATIONS, DEFAULT_LOCATIONS)

    fun getProps(context: Context): List<String> = getList(context, KEY_PROPS, DEFAULT_PROPS)
    fun addProp(context: Context, value: String) = addItem(context, KEY_PROPS, DEFAULT_PROPS, value)
    fun removeProp(context: Context, value: String) =
        removeItem(context, KEY_PROPS, DEFAULT_PROPS, value)

    fun resetProps(context: Context) = saveList(context, KEY_PROPS, DEFAULT_PROPS)

    fun getMoods(context: Context): List<String> = getList(context, KEY_MOODS, DEFAULT_MOODS)
    fun addMood(context: Context, value: String) = addItem(context, KEY_MOODS, DEFAULT_MOODS, value)
    fun removeMood(context: Context, value: String) =
        removeItem(context, KEY_MOODS, DEFAULT_MOODS, value)

    fun resetMoods(context: Context) = saveList(context, KEY_MOODS, DEFAULT_MOODS)
}