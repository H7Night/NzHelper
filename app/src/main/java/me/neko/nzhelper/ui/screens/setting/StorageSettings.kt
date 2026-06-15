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