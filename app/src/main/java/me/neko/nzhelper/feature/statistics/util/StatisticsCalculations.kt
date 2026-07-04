package me.neko.nzhelper.feature.statistics.util

import android.annotation.SuppressLint
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.HeatmapData
import me.neko.nzhelper.feature.statistics.model.HeatmapDay
import me.neko.nzhelper.feature.statistics.model.HeatmapWeek
import me.neko.nzhelper.feature.statistics.model.LatestSessionInfo
import me.neko.nzhelper.feature.statistics.model.PeriodData
import me.neko.nzhelper.feature.statistics.model.PeriodOverview
import me.neko.nzhelper.feature.statistics.model.PeriodType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ── 热力图数据 ──
fun calculateHeatmapData(
    sessions: List<Session>,
    now: LocalDateTime
): HeatmapData {
    val today = now.toLocalDate()
    val weeksToShow = 14
    val dayOfWeek = today.dayOfWeek.value
    val mondayThisWeek = today.minusDays((dayOfWeek - 1).toLong())
    val startMonday = mondayThisWeek.minusWeeks((weeksToShow - 1).toLong())

    val sessionMap = sessions
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { e -> e.value.size to e.value.sumOf { it.duration } }

    val weeks = mutableListOf<HeatmapWeek>()
    val monthLabels = mutableListOf<Pair<Int, String>>()
    var lastMonth = -1
    var activeDays = 0
    var totalDays = 0

    for (weekIndex in 0 until weeksToShow) {
        val weekStart = startMonday.plusWeeks(weekIndex.toLong())

        if (weekStart.monthValue != lastMonth) {
            monthLabels.add(weekIndex to "${weekStart.monthValue}月")
            lastMonth = weekStart.monthValue
        }

        val days = (0 until 7).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val isFuture = date > today
            val (count, duration) = sessionMap[date] ?: (0 to 0)
            if (!isFuture) {
                totalDays++
                if (count > 0) activeDays++
            }
            HeatmapDay(date, count, duration, isFuture)
        }
        weeks.add(HeatmapWeek(days))
    }

    val maxCount = sessionMap.values.maxOfOrNull { it.first } ?: 0

    return HeatmapData(
        weeks = weeks,
        monthLabels = monthLabels,
        weekCount = weeksToShow,
        maxCount = maxCount,
        activeDays = activeDays,
        totalDays = totalDays
    )
}

// ── 趋势图数据 ──
fun calculateTrendData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val today = now.toLocalDate()
    val dayOfWeek = today.dayOfWeek.value
    val mondayThisWeek = today.minusDays((dayOfWeek - 1).toLong())
    val weeksToShow = 12
    val result = mutableListOf<Pair<String, Float>>()

    for (i in weeksToShow - 1 downTo 0) {
        val weekStart = mondayThisWeek.minusWeeks(i.toLong())
        val weekEnd = weekStart.plusDays(6)
        val totalMinutes = sessions
            .filter { it.timestamp.toLocalDate() in weekStart..weekEnd }
            .sumOf { it.duration } / 60f
        val label = if (i == 0) "本周" else "${weekStart.monthValue}/${weekStart.dayOfMonth}"
        result.add(label to totalMinutes)
    }
    return result
}

fun calculatePeriodData(
    sessions: List<Session>,
    now: LocalDateTime,
    type: PeriodType
): PeriodData {
    val filteredSessions = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    if (filteredSessions.isEmpty()) return PeriodData(0, 0f, emptyList())

    val totalSeconds = filteredSessions.sumOf { it.duration }
    val avgMinutes = totalSeconds.toFloat() / (60 * filteredSessions.size)

    val chartData = when (type) {
        PeriodType.WEEK -> calculateWeeklyChartData(filteredSessions, now)
        PeriodType.MONTH -> calculateMonthlyChartData(filteredSessions, now)
        PeriodType.YEAR -> calculateYearlyChartData(filteredSessions, now)
    }

    return PeriodData(totalSeconds, avgMinutes, chartData)
}

fun isWithinPeriod(
    timestamp: LocalDateTime,
    now: LocalDateTime,
    type: PeriodType
): Boolean {
    val start = when (type) {
        PeriodType.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
            .atStartOfDay()

        PeriodType.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        PeriodType.YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay()
    }
    return timestamp >= start
}

private fun calculateWeeklyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val monday = now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    val dailyMap = sessions
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }

    return weekDays.map { date ->
        val label = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
            DayOfWeek.SUNDAY -> "日"
            else -> ""
        }
        label to (dailyMap[date] ?: 0f)
    }
}

private fun calculateMonthlyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    val firstDay = now.withDayOfMonth(1).toLocalDate()

    return sessions
        .filter { it.timestamp.toLocalDate() >= firstDay }
        .groupBy { it.timestamp.toLocalDate() }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }
        .filter { it.value > 0f }
        .entries
        .sortedBy { it.key }
        .map { entry ->
            entry.key.format(DateTimeFormatter.ofPattern("dd")) to entry.value
        }
}

private fun calculateYearlyChartData(
    sessions: List<Session>,
    now: LocalDateTime
): List<Pair<String, Float>> {
    return sessions
        .filter { it.timestamp.year == now.year }
        .groupBy { YearMonth.from(it.timestamp) }
        .mapValues { it.value.sumOf { s -> s.duration } / 60f }
        .filter { it.value > 0f }
        .entries
        .sortedBy { it.key }
        .map { entry ->
            "${entry.key.monthValue}月" to entry.value
        }
}

fun calculateLatestInfo(sessions: List<Session>): LatestSessionInfo? {
    if (sessions.isEmpty()) return null

    val latest = sessions.maxByOrNull { it.timestamp }!!
    val lastDate = latest.timestamp.toLocalDate()
    val today = LocalDateTime.now().toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(lastDate, today)

    val displayDate = when (daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        else -> lastDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }

    val time = latest.timestamp.format(DateTimeFormatter.ofPattern("a h:mm", Locale.CHINA))
    val durationText = formatDuration(latest.duration)

    val isErrorState = if (daysAgo <= 1) {
        val countOnLastDate = sessions.count { it.timestamp.toLocalDate() == lastDate }
        if (countOnLastDate >= 2) {
            true
        } else {
            val dayBeforeLast = lastDate.minusDays(1)
            val hasSessionDayBefore = sessions.any { it.timestamp.toLocalDate() == dayBeforeLast }
            hasSessionDayBefore
        }
    } else {
        false
    }

    val detailText = getRandomComment(daysAgo, isErrorState)

    return LatestSessionInfo(displayDate, time, durationText, daysAgo, detailText, isErrorState)
}

private fun getRandomComment(days: Long, isError: Boolean): String {
    return if (isError) {
        when (days) {
            0L -> listOf(
                "今日不宜贪多，请注意节制",
                "频率过高，身体需要休息",
                "透支精力，明天暂停吧",
                "为了健康，请适当克制"
            ).random()

            1L -> listOf(
                "连续高强度，身体吃不消的",
                "昨日频率较高，今日需休养",
                "注意节奏，不要贪多",
                "过于频繁，容易疲劳"
            ).random()

            else -> listOf(
                "近期频率偏高，建议克制",
                "注意身体健康，不要过度"
            ).random()
        }
    } else {
        when (days) {
            0L -> listOf(
                "适度释放，身心舒畅",
                "保持规律，劳逸结合",
                "状态不错，继续保持",
                "记得多喝水，补充水分"
            ).random()

            1L -> listOf(
                "昨日适度，状态不错",
                "间隔合理，精力充沛",
                "节奏很好，享受生活"
            ).random()

            2L -> listOf(
                "休息了两天，精力恢复",
                "身体状态满分，蓄势待发",
                "休养生息，很有活力"
            ).random()

            else -> listOf(
                "很久没活动了，身体状态极佳",
                "保持健康的生活方式",
                "精力充沛，充满活力"
            ).random()
        }
    }
}

@SuppressLint("DefaultLocale")
fun calculatePeriodOverview(
    sessions: List<Session>,
    now: LocalDateTime,
    type: PeriodType,
    label: String
): PeriodOverview {
    val filtered = sessions.filter { isWithinPeriod(it.timestamp, now, type) }

    val prevLabel = when (type) {
        PeriodType.WEEK -> "上周"
        PeriodType.MONTH -> "上月"
        PeriodType.YEAR -> "去年"
    }

    if (filtered.isEmpty()) {
        return PeriodOverview(
            periodLabel = label, count = 0, totalDurationSeconds = 0,
            longestDurationSeconds = 0, longestSessionDisplayDate = "",
            longestSessionEndTime = "", mostUsedProps = "", mostUsedPropsCount = 0,
            mostCommonMood = "", mostCommonMoodCount = 0,
            mostCommonLocation = "", mostCommonLocationCount = 0,
            avgRating = 0f, movieCount = 0, climaxCount = 0,
            countComparison = "", durationComparison = "",
            propsComparison = "", moodComparison = "", locationComparison = "",
            avgRatingComparison = "", movieComparison = "", climaxComparison = ""
        )
    }

    val startCurrent = when (type) {
        PeriodType.WEEK -> now.minusDays(now.dayOfWeek.value.toLong() - 1).toLocalDate()
            .atStartOfDay()

        PeriodType.MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        PeriodType.YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay()
    }
    val startPrev = when (type) {
        PeriodType.WEEK -> startCurrent.minusWeeks(1)
        PeriodType.MONTH -> startCurrent.minusMonths(1)
        PeriodType.YEAR -> startCurrent.minusYears(1)
    }
    val prevSessions = sessions.filter { it.timestamp in startPrev..<startCurrent }
    val prevCount = prevSessions.size

    val countComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val diff = filtered.size - prevCount
        when {
            diff > 0 -> "较${prevLabel}多${diff}次"
            diff < 0 -> "较${prevLabel}少${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    val totalDuration = filtered.sumOf { it.duration }
    val prevTotalDuration = prevSessions.sumOf { it.duration }
    val durationComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val diff = totalDuration - prevTotalDuration
        when {
            diff > 0 -> "较${prevLabel}多${formatDuration(diff)}"
            diff < 0 -> "较${prevLabel}少${formatDuration(-diff)}"
            else -> "与${prevLabel}相同"
        }
    }

    val longest = filtered.maxByOrNull { it.duration }!!
    val endDateTime = longest.timestamp.plusSeconds(longest.duration.toLong())

    val mostUsedProps = filtered.groupingBy { it.props }.eachCount().maxByOrNull { it.value }
    val prevMostUsedProps =
        prevSessions.groupingBy { it.props }.eachCount().maxByOrNull { it.value }

    val propsComparison = if (prevMostUsedProps == null) {
        "${prevLabel}无记录"
    } else {
        val prevPropsName = prevMostUsedProps.key.ifEmpty { "无" }
        if (prevMostUsedProps.key == mostUsedProps?.key) {
            "${prevLabel}也为：$prevPropsName (${prevMostUsedProps.value}次)"
        } else {
            "${prevLabel}为：$prevPropsName (${prevMostUsedProps.value}次)"
        }
    }

    val mostCommonMood = filtered.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }
    val prevMostCommonMood =
        prevSessions.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }

    val moodComparison = if (prevMostCommonMood == null) {
        "${prevLabel}无记录"
    } else {
        val prevMoodName = prevMostCommonMood.key.ifEmpty { "无" }
        if (prevMostCommonMood.key == mostCommonMood?.key) {
            "${prevLabel}也为：$prevMoodName (${prevMostCommonMood.value}次)"
        } else {
            "${prevLabel}为：$prevMoodName (${prevMostCommonMood.value}次)"
        }
    }

    val mostCommonLocation = filtered
        .mapNotNull { it.location.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
    val prevMostCommonLocation = prevSessions
        .mapNotNull { it.location.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }

    val locationComparison = if (prevMostCommonLocation == null) {
        "${prevLabel}无记录"
    } else {
        if (prevMostCommonLocation.key == mostCommonLocation?.key) {
            "${prevLabel}也为：${prevMostCommonLocation.key} (${prevMostCommonLocation.value}次)"
        } else {
            "${prevLabel}为：${prevMostCommonLocation.key} (${prevMostCommonLocation.value}次)"
        }
    }

    val avgRating = filtered.map { it.rating }.average().toFloat()
    val avgRatingComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevAvgRating = prevSessions.map { it.rating }.average().toFloat()
        val diff = avgRating - prevAvgRating
        when {
            diff > 0.05f -> "较${prevLabel}高 ${String.format("%.1f", diff)}"
            diff < -0.05f -> "较${prevLabel}低 ${String.format("%.1f", -diff)}"
            else -> "与${prevLabel}持平"
        }
    }

    val movieCount = filtered.count { it.watchedMovie }
    val movieComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevMovieCount = prevSessions.count { it.watchedMovie }
        val diff = movieCount - prevMovieCount
        when {
            diff > 0 -> "较${prevLabel}多 ${diff}次"
            diff < 0 -> "较${prevLabel}少 ${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    val climaxCount = filtered.count { it.climax }
    val climaxComparison = if (prevCount == 0) {
        "${prevLabel}无记录"
    } else {
        val prevClimaxCount = prevSessions.count { it.climax }
        val diff = climaxCount - prevClimaxCount
        when {
            diff > 0 -> "较${prevLabel}多 ${diff}次"
            diff < 0 -> "较${prevLabel}少 ${-diff}次"
            else -> "与${prevLabel}相同"
        }
    }

    return PeriodOverview(
        periodLabel = label,
        count = filtered.size,
        totalDurationSeconds = totalDuration,
        longestDurationSeconds = longest.duration,
        longestSessionDisplayDate = longest.timestamp.format(
            DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
        ),
        longestSessionEndTime = endDateTime.format(
            DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
        ),
        mostUsedProps = mostUsedProps?.key ?: "",
        mostUsedPropsCount = mostUsedProps?.value ?: 0,
        mostCommonMood = mostCommonMood?.key ?: "",
        mostCommonMoodCount = mostCommonMood?.value ?: 0,
        mostCommonLocation = mostCommonLocation?.key ?: "未记录",
        mostCommonLocationCount = mostCommonLocation?.value ?: 0,
        avgRating = avgRating,
        movieCount = movieCount,
        climaxCount = climaxCount,
        countComparison = countComparison,
        durationComparison = durationComparison,
        propsComparison = propsComparison,
        moodComparison = moodComparison,
        locationComparison = locationComparison,
        avgRatingComparison = avgRatingComparison,
        movieComparison = movieComparison,
        climaxComparison = climaxComparison
    )
}

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}小时${minutes}分"
    } else {
        "${minutes}分钟"
    }
}

fun buildTotalStatStatus(sessions: List<Session>): String {
    if (sessions.isEmpty()) return ""
    if (sessions.size < 2) return "刚开始记录，保持适度"

    val dates = sessions.map { it.timestamp.toLocalDate() }
    val earliest = dates.min()
    val latest = dates.max()

    val daysSpan = ChronoUnit.DAYS.between(earliest, latest).coerceAtLeast(1)

    val frequency = sessions.size.toDouble() / daysSpan

    return when {
        frequency >= 1.0 -> "平均每天一次以上，频率偏高，建议适度节制"
        frequency >= 0.3 -> "平均两三天一次，较为频繁，注意休息"
        frequency >= 0.14 -> "平均一周左右一次，频率适中，身心健康"
        else -> "频率较低，精力充沛"
    }
}