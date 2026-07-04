package me.neko.nzhelper.feature.statistics.model

import java.time.LocalDate

enum class PeriodType { WEEK, MONTH, YEAR }

data class PeriodData(
    val totalDurationSeconds: Int,
    val avgDurationMinutes: Float,
    val chartData: List<Pair<String, Float>>
)

data class TotalStats(
    val totalCount: Int,
    val totalSeconds: Int,
    val avgMinutes: Float,
    val weekCount: Int,
    val monthCount: Int,
    val yearCount: Int
)

data class LatestSessionInfo(
    val displayDate: String,
    val time: String,
    val durationText: String,
    val daysAgo: Long,
    val detailText: String,
    val isErrorState: Boolean
)

data class PeriodOverview(
    val periodLabel: String,
    val count: Int,
    val totalDurationSeconds: Int,
    val longestDurationSeconds: Int,
    val longestSessionDisplayDate: String,
    val longestSessionEndTime: String,
    val mostUsedProps: String,
    val mostUsedPropsCount: Int,
    val mostCommonMood: String,
    val mostCommonMoodCount: Int,
    val mostCommonLocation: String,
    val mostCommonLocationCount: Int,
    val avgRating: Float,
    val movieCount: Int,
    val climaxCount: Int,
    val countComparison: String = "",
    val durationComparison: String = "",
    val propsComparison: String = "",
    val moodComparison: String = "",
    val locationComparison: String = "",
    val avgRatingComparison: String = "",
    val movieComparison: String = "",
    val climaxComparison: String = ""
)

data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val monthLabels: List<Pair<Int, String>>,
    val weekCount: Int,
    val maxCount: Int,
    val activeDays: Int,
    val totalDays: Int
)

data class HeatmapWeek(
    val days: List<HeatmapDay>
)

data class HeatmapDay(
    val date: LocalDate,
    val count: Int,
    val totalDurationSeconds: Int,
    val isFuture: Boolean
)