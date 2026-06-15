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
import me.neko.nzhelper.ui.screens.setting.StorageSettings
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SessionRepository {

    private const val PREFS_NAME = "sessions_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val EXTERNAL_FILENAME = "nzHelper_data.json"

    private val gson = NzApplication.gson
    private val sessionsTypeToken = object : TypeToken<List<Session>>() {}.type

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

    /**
     * 切换存储模式并迁移数据
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

            val json = readJson(context) ?: ""

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

            StorageSettings.setMode(context, newMode)
            if (newMode == StorageSettings.MODE_EXTERNAL) {
                StorageSettings.setExternalPath(context, newPath)
            }

            if (json.isNotEmpty()) {
                writeJson(context, json)
            }

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