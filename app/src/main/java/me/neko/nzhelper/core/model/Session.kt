package me.neko.nzhelper.core.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Session(
    @SerializedName("timestamp") val timestamp: LocalDateTime,
    @SerializedName("duration") val duration: Int,
    @SerializedName("remark") val remark: String,
    @SerializedName("location") val location: String,
    @SerializedName("watchedMovie") val watchedMovie: Boolean,
    @SerializedName("climax") val climax: Boolean,
    @SerializedName("rating") val rating: Float,
    @SerializedName("mood") val mood: String,
    @SerializedName("props") val props: String
)

data class RecycleBinItem(
    @SerializedName("session") val session: Session,
    @SerializedName("deletedTimestamp") val deletedTimestamp: Long = System.currentTimeMillis()
)

data class WebDavBackupPayload(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportedAt") val exportedAt: Long,
    @SerializedName("sessions") val sessions: List<Session> = emptyList(),
    @SerializedName("recycleBin") val recycleBin: List<RecycleBinItem> = emptyList()
)