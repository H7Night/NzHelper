package me.neko.nzhelper.ui.screens.setting

import android.content.Context
import androidx.core.content.edit

object WebDavSettings {
    private const val PREFS_NAME = "webdav_prefs"
    private const val KEY_URL = "url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMOTE_PATH = "remote_path"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val DEFAULT_REMOTE_PATH = "/NzHelper/nzHelper_backup.json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUrl(context: Context): String =
        prefs(context).getString(KEY_URL, "") ?: ""

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun getPassword(context: Context): String =
        prefs(context).getString(KEY_PASSWORD, "") ?: ""

    fun getRemotePath(context: Context): String =
        prefs(context).getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH) ?: DEFAULT_REMOTE_PATH

    fun isAutoBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_BACKUP, false)

    fun isConfigured(context: Context): Boolean =
        getUrl(context).isNotBlank() && getUsername(context).isNotBlank()

    fun getLastBackupTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_BACKUP_TIME, 0L)

    fun setLastBackupTime(context: Context, time: Long) {
        prefs(context).edit { putLong(KEY_LAST_BACKUP_TIME, time) }
    }

    fun save(
        context: Context,
        url: String,
        username: String,
        password: String,
        remotePath: String
    ) {
        prefs(context).edit {
            putString(KEY_URL, url.trimEnd('/'))
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putString(KEY_REMOTE_PATH, remotePath.ifBlank { DEFAULT_REMOTE_PATH })
        }
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_BACKUP, enabled) }
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }
}