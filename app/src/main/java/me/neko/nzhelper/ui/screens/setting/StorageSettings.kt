package me.neko.nzhelper.ui.screens.setting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.edit

object StorageSettings {
    const val MODE_INTERNAL = "internal"
    const val MODE_EXTERNAL = "external"
    const val DEFAULT_EXTERNAL_PATH = "/storage/emulated/0/NzHelper"
    private const val PREFS = "settings_prefs"

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("storage_mode", MODE_INTERNAL) ?: MODE_INTERNAL

    fun setMode(context: Context, mode: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString("storage_mode", mode) }

    fun getExternalPath(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("external_storage_path", DEFAULT_EXTERNAL_PATH) ?: DEFAULT_EXTERNAL_PATH

    fun setExternalPath(context: Context, path: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString("external_storage_path", path) }

    fun hasExternalStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

object RecycleBinSettings {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_AUTO_CLEAN_ENABLED = "recycle_bin_auto_clean_enabled"

    const val RETENTION_DAYS = 30 // 30天

    fun isAutoCleanEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CLEAN_ENABLED, false)
    }

    fun setAutoCleanEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_AUTO_CLEAN_ENABLED, enabled) }
    }
}