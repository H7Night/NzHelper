package me.neko.nzhelper.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.ui.screens.setting.RecycleBinSettings
import me.neko.nzhelper.ui.screens.setting.StorageSettings
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_RECYCLE_BIN = "recycle_bin"
    private const val EXTERNAL_FILENAME = "nzHelper_data.json"
    private const val EXTERNAL_RECYCLE_FILENAME = "nzHelper_recycle.json"

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type
    private val recycleBinTypeToken = object : TypeToken<List<RecycleBinItem>>() {}.type

    private fun readJson(context: Context): String? {
        return if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            val file = File(dir, EXTERNAL_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, null)
        }
    }

    /**
     * 从指定模式/路径读取数据（不受全局设置影响，用于迁移前预读目标位置数据）
     */
    private fun readJsonFromTarget(mode: String, path: String, context: Context): String? {
        return if (mode == StorageSettings.MODE_EXTERNAL) {
            val dir = File(path)
            val file = File(dir, EXTERNAL_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, null)
        }
    }

    private fun writeJson(context: Context, json: String) {
        if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            if (!dir.exists()) dir.mkdirs()
            File(dir, EXTERNAL_FILENAME).writeText(json)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_SESSIONS, json) }
        }
    }

    private fun parseSessionsJson(context: Context, json: String): List<Session> {
        return try {
            val root = JsonParser.parseString(json)
            if (root.isJsonArray && root.asJsonArray.size() > 0) {
                val firstElem = root.asJsonArray[0]
                if (firstElem.isJsonObject) {
                    val obj = firstElem.asJsonObject
                    if (!obj.has("timestamp")) {
                        val migrated = migrateObfuscatedData(root.asJsonArray)
                        val correctedJson = gson.toJson(migrated)
                        writeJson(context, correctedJson)
                        return migrated
                    }
                }
            }
            gson.fromJson(json, sessionsTypeToken) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun readRecycleBinJson(context: Context): String? {
        return if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            val file = File(dir, EXTERNAL_RECYCLE_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
    }

    private fun readRecycleBinJsonFromTarget(
        mode: String,
        path: String,
        context: Context
    ): String? {
        return if (mode == StorageSettings.MODE_EXTERNAL) {
            val dir = File(path)
            val file = File(dir, EXTERNAL_RECYCLE_FILENAME)
            if (file.exists()) file.readText() else null
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECYCLE_BIN, null)
        }
    }

    private fun writeRecycleBinJson(context: Context, json: String) {
        if (StorageSettings.getMode(context) == StorageSettings.MODE_EXTERNAL) {
            val dir = File(StorageSettings.getExternalPath(context))
            if (!dir.exists()) dir.mkdirs()
            File(dir, EXTERNAL_RECYCLE_FILENAME).writeText(json)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit { putString(KEY_RECYCLE_BIN, json) }
        }
    }

    private fun parseRecycleBinJson(json: String): List<RecycleBinItem> {
        return try {
            val items =
                gson.fromJson<List<RecycleBinItem>>(json, recycleBinTypeToken) ?: emptyList()
            items
        } catch (_: Exception) {
            try {
                val oldSessions =
                    gson.fromJson<List<Session>>(json, sessionsTypeToken) ?: emptyList()
                oldSessions.map {
                    RecycleBinItem(
                        session = it,
                        deletedTimestamp = System.currentTimeMillis()
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun loadSessions(context: Context): List<Session> = withContext(Dispatchers.IO) {
        val json = readJson(context)
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            parseSessionsJson(context, json)
        }
    }

    suspend fun saveSessions(context: Context, sessions: List<Session>) =
        withContext(Dispatchers.IO) {
            val json = gson.toJson(sessions)
            writeJson(context, json)
        }

    suspend fun loadRecycleBin(context: Context): List<RecycleBinItem> =
        withContext(Dispatchers.IO) {
            val json = readRecycleBinJson(context)
            if (json.isNullOrEmpty()) emptyList() else parseRecycleBinJson(json)
        }

    suspend fun moveAllToRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        val currentJson = readJson(context)
        val currentSessions = if (!currentJson.isNullOrEmpty()) {
            parseSessionsJson(context, currentJson)
        } else {
            emptyList()
        }
        if (currentSessions.isEmpty()) return@withContext

        val recycleBinJson = readRecycleBinJson(context)
        val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
            parseRecycleBinJson(recycleBinJson)
        } else {
            emptyList()
        }

        val now = System.currentTimeMillis()
        val newItems = currentSessions.map { RecycleBinItem(session = it, deletedTimestamp = now) }

        val newRecycleBin = (currentRecycleBin + newItems).distinctBy { it.session.timestamp }
        writeRecycleBinJson(context, gson.toJson(newRecycleBin))
        writeJson(context, gson.toJson(emptyList<Session>()))
    }

    /**
     * 将指定记录移入回收站（用于单条/多条删除）
     */
    suspend fun moveSessionsToRecycleBin(context: Context, sessionsToMove: List<Session>) =
        withContext(Dispatchers.IO) {
            if (sessionsToMove.isEmpty()) return@withContext

            val currentJson = readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val timestampsToMove = sessionsToMove.map { it.timestamp }.toSet()
            val remaining = currentSessions.filter { it.timestamp !in timestampsToMove }

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val now = System.currentTimeMillis()
            val newItems =
                sessionsToMove.map { RecycleBinItem(session = it, deletedTimestamp = now) }
            val newRecycleBin = (currentRecycleBin + newItems).distinctBy { it.session.timestamp }

            writeRecycleBinJson(context, gson.toJson(newRecycleBin))
            writeJson(context, gson.toJson(remaining))
        }

    /**
     * 从回收站恢复记录
     */
    suspend fun restoreFromRecycleBin(context: Context, itemsToRestore: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToRestore.isEmpty()) return@withContext

            val currentJson = readJson(context)
            val currentSessions = if (!currentJson.isNullOrEmpty()) {
                parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val sessionsToRestore = itemsToRestore.map { it.session }
            val timestampsToRestore = sessionsToRestore.map { it.timestamp }.toSet()

            val mergedSessions = (currentSessions + sessionsToRestore).distinctBy { it.timestamp }
            val remainingRecycleBin =
                currentRecycleBin.filter { it.session.timestamp !in timestampsToRestore }

            writeJson(context, gson.toJson(mergedSessions))
            writeRecycleBinJson(context, gson.toJson(remainingRecycleBin))
        }

    /**
     * 从回收站永久删除指定记录
     */
    suspend fun deleteFromRecycleBin(context: Context, itemsToDelete: List<RecycleBinItem>) =
        withContext(Dispatchers.IO) {
            if (itemsToDelete.isEmpty()) return@withContext

            val recycleBinJson = readRecycleBinJson(context)
            val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
                parseRecycleBinJson(recycleBinJson)
            } else {
                emptyList()
            }

            val timestampsToDelete = itemsToDelete.map { it.session.timestamp }.toSet()
            val remaining = currentRecycleBin.filter { it.session.timestamp !in timestampsToDelete }

            writeRecycleBinJson(context, gson.toJson(remaining))
        }

    /**
     * 清空回收站（永久删除所有回收站记录）
     */
    suspend fun clearRecycleBin(context: Context) = withContext(Dispatchers.IO) {
        writeRecycleBinJson(context, gson.toJson(emptyList<RecycleBinItem>()))
    }

    /**
     * 清理过期的回收站记录
     */
    suspend fun cleanExpiredRecycleBinItems(context: Context) = withContext(Dispatchers.IO) {

        if (!RecycleBinSettings.isAutoCleanEnabled(context)) return@withContext

        val retentionDays = RecycleBinSettings.RETENTION_DAYS
        val retentionMillis = retentionDays * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        val recycleBinJson = readRecycleBinJson(context)
        val currentRecycleBin = if (!recycleBinJson.isNullOrEmpty()) {
            parseRecycleBinJson(recycleBinJson)
        } else {
            emptyList()
        }

        val remaining = currentRecycleBin.filter { item ->
            (currentTime - item.deletedTimestamp) < retentionMillis
        }

        if (remaining.size != currentRecycleBin.size) {
            writeRecycleBinJson(context, gson.toJson(remaining))
        }
    }

    /**
     * 切换存储模式并合并迁移数据
     * @return true 切换成功，false 切换失败（路径不可写等）
     */
    suspend fun switchStorageMode(
        context: Context,
        newMode: String,
        newPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentMode = StorageSettings.getMode(context)
            val currentPath = StorageSettings.getExternalPath(context)

            if (currentMode == newMode &&
                (newMode != StorageSettings.MODE_EXTERNAL || currentPath == newPath)
            ) {
                return@withContext true
            }

            if (newMode == StorageSettings.MODE_EXTERNAL) {
                val dir = File(newPath)
                if (!dir.exists() && !dir.mkdirs()) return@withContext false
                val testFile = File(dir, ".nzhelper_write_test")
                try {
                    testFile.writeText("test")
                    testFile.delete()
                } catch (_: Exception) {
                    return@withContext false
                }
            }

            val currentJson = readJson(context) ?: ""
            val currentSessions = if (currentJson.isNotEmpty()) {
                parseSessionsJson(context, currentJson)
            } else {
                emptyList()
            }

            val targetJson = readJsonFromTarget(newMode, newPath, context) ?: ""
            val targetSessions = if (targetJson.isNotEmpty()) {
                parseSessionsJson(context, targetJson)
            } else {
                emptyList()
            }

            val mergedSessions = (currentSessions + targetSessions)
                .distinctBy { it.timestamp }

            // ===== 合并回收站数据 =====
            val currentRecycleBinJson = readRecycleBinJson(context) ?: ""
            val currentRecycleBin = if (currentRecycleBinJson.isNotEmpty()) {
                parseRecycleBinJson(currentRecycleBinJson)
            } else {
                emptyList()
            }

            val targetRecycleBinJson = readRecycleBinJsonFromTarget(newMode, newPath, context) ?: ""
            val targetRecycleBin = if (targetRecycleBinJson.isNotEmpty()) {
                parseRecycleBinJson(targetRecycleBinJson)
            } else {
                emptyList()
            }

            val mergedRecycleBin = (currentRecycleBin + targetRecycleBin)
                .distinctBy { it.session.timestamp }

            StorageSettings.setMode(context, newMode)
            if (newMode == StorageSettings.MODE_EXTERNAL) {
                StorageSettings.setExternalPath(context, newPath)
            }

            saveSessions(context, mergedSessions)
            writeRecycleBinJson(context, gson.toJson(mergedRecycleBin))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将 R8 混淆后的数据映射回正确结构
     */
    private fun migrateObfuscatedData(array: com.google.gson.JsonArray): List<Session> {
        val result = mutableListOf<Session>()
        for (elem in array) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            try {
                val aStr = obj.get("a")?.asString
                if (aStr.isNullOrEmpty()) continue
                val timestamp = LocalDateTime.parse(aStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                val duration = try {
                    obj.get("b")?.asInt ?: 0
                } catch (_: Exception) {
                    0
                }
                val remark = try {
                    if (obj.has("c") && !obj.get("c").isJsonNull) obj.get("c").asString else ""
                } catch (_: Exception) {
                    ""
                }
                val location = try {
                    if (obj.has("d") && !obj.get("d").isJsonNull) obj.get("d").asString else ""
                } catch (_: Exception) {
                    ""
                }
                val watchedMovie = try {
                    obj.get("e")?.asBoolean ?: false
                } catch (_: Exception) {
                    false
                }
                val climax = try {
                    obj.get("f")?.asBoolean ?: false
                } catch (_: Exception) {
                    false
                }
                val rating = try {
                    (obj.get("g")?.asFloat ?: 3f).coerceIn(0f, 5f)
                } catch (_: Exception) {
                    3f
                }
                val mood = try {
                    if (obj.has("h") && !obj.get("h").isJsonNull) obj.get("h").asString else "平静"
                } catch (_: Exception) {
                    "平静"
                }
                val props = try {
                    if (obj.has("i") && !obj.get("i").isJsonNull) obj.get("i").asString else "手"
                } catch (_: Exception) {
                    "手"
                }

                result.add(
                    Session(
                        timestamp = timestamp,
                        duration = duration,
                        remark = remark,
                        location = location,
                        watchedMovie = watchedMovie,
                        climax = climax,
                        rating = rating,
                        mood = mood,
                        props = props
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * 从指定 Uri 解析导入的 JSON 文件
     */
    suspend fun parseImportFile(
        context: Context,
        uri: Uri,
        gson: Gson,
        listType: java.lang.reflect.Type
    ): List<Session> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Session>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonStr = inputStream.bufferedReader().readText()
                try {
                    val list: List<Session> = gson.fromJson(jsonStr, listType)
                    result.addAll(list)
                    return@withContext result
                } catch (_: Exception) {
                    try {
                        val root = JsonParser.parseString(jsonStr).asJsonArray
                        for (elem in root) {
                            if (elem.isJsonArray) {
                                val arr = elem.asJsonArray
                                val timeStr = arr[0].asString
                                val timestamp = LocalDateTime.parse(
                                    timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                )
                                val duration = if (arr.size() > 1) arr[1].asInt else 0
                                val remark =
                                    if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                                val location =
                                    if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else ""
                                val watchedMovie = if (arr.size() > 4) arr[4].asBoolean else false
                                val climax = if (arr.size() > 5) arr[5].asBoolean else false
                                val rating =
                                    if (arr.size() > 6 && !arr[6].isJsonNull) arr[6].asFloat.coerceIn(
                                        0f,
                                        5f
                                    ) else 3f
                                val mood =
                                    if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静"
                                val props =
                                    if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"
                                result.add(
                                    Session(
                                        timestamp = timestamp,
                                        duration = duration,
                                        remark = remark,
                                        location = location,
                                        watchedMovie = watchedMovie,
                                        climax = climax,
                                        rating = rating,
                                        mood = mood,
                                        props = props
                                    )
                                )
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }
}