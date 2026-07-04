package me.neko.nzhelper.core.webdav

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
    private const val BACKUP_FILE_NAME = "nzHelper_backup.json"
    private const val DEFAULT_REMOTE_PATH = "/NzHelper"
    private const val KEY_MIGRATED_V2 = "path_migrated_v2"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUrl(context: Context): String =
        prefs(context).getString(KEY_URL, "") ?: ""

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "") ?: ""

    fun getPassword(context: Context): String =
        prefs(context).getString(KEY_PASSWORD, "") ?: ""

    fun getRemotePath(context: Context): String {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_MIGRATED_V2, false)) {
            var current =
                prefs.getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH) ?: DEFAULT_REMOTE_PATH
            if (current.endsWith("/$BACKUP_FILE_NAME")) {
                current = current.removeSuffix("/$BACKUP_FILE_NAME").ifBlank { DEFAULT_REMOTE_PATH }
                prefs.edit { putString(KEY_REMOTE_PATH, current) }
            }
            prefs.edit { putBoolean(KEY_MIGRATED_V2, true) }
            return current
        }
        return prefs.getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH) ?: DEFAULT_REMOTE_PATH
    }

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
            val normalized = remotePath.trim().trimEnd('/').ifBlank { DEFAULT_REMOTE_PATH }
            putString(KEY_REMOTE_PATH, normalized)
        }
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_BACKUP, enabled) }
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }
}