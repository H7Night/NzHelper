package me.neko.nzhelper.data

import java.time.LocalDateTime

data class SessionFormState(
    val remark: String = "",
    val location: String = "",
    val watchedMovie: Boolean = false,
    val climax: Boolean = false,
    val rating: Float = 3f,
    val mood: String = "平静",
    val props: String = "手",
    val durationHour: String = "",
    val durationMinute: String = "",
    val durationSecond: String = "",
    val manualYear: Int = 0,
    val manualMonth: Int = 0,
    val manualDay: Int = 0,
    val manualHour: Int = 0,
    val manualMinute: Int = 0
) {
    val manualDurationSeconds: Int
        get() = (durationHour.toIntOrNull() ?: 0) * 3600 +
                (durationMinute.toIntOrNull() ?: 0) * 60 +
                (durationSecond.toIntOrNull() ?: 0)

    fun toLocalDateTime(): LocalDateTime {
        return LocalDateTime.of(
            manualYear,
            manualMonth,
            manualDay,
            manualHour,
            manualMinute,
            0,
            0
        )
    }
}