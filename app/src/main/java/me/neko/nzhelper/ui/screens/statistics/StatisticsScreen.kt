package me.neko.nzhelper.ui.screens.statistics

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(isActive: Boolean = false) {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
            sessions.clear()
            sessions.addAll(loaded)
        }
    }

    val currentTime = LocalDateTime.now()

    val weekData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.WEEK) }
    }
    val monthData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.MONTH) }
    }
    val yearData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.YEAR) }
    }

    val totalStats by remember(sessions) {
        derivedStateOf {
            if (sessions.isEmpty()) {
                TotalStats(0, 0, 0f, 0, 0, 0)
            } else {
                val totalCount = sessions.size
                val totalSeconds = sessions.sumOf { it.duration }
                val avgMinutes =
                    if (totalCount > 0) totalSeconds.toFloat() / (60 * totalCount) else 0f

                TotalStats(
                    totalCount = totalCount,
                    totalSeconds = totalSeconds,
                    avgMinutes = avgMinutes,
                    weekCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.WEEK)
                    },
                    monthCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.MONTH)
                    },
                    yearCount = sessions.count {
                        isWithinPeriod(it.timestamp, currentTime, PeriodType.YEAR)
                    }
                )
            }
        }
    }

    val latestInfo by remember(sessions) {
        derivedStateOf { calculateLatestInfo(sessions) }
    }

    var selectedOverview by remember { mutableStateOf<PeriodOverview?>(null) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (sessions.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LatestSessionCard(
                            latestInfo = latestInfo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        TotalStatCard(
                            stats = totalStats,
                            sessions = sessions,
                            onPeriodClick = { type, label ->
                                selectedOverview = calculatePeriodOverview(
                                    sessions, currentTime, type, label
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        PeriodChartCard(
                            weekData = weekData,
                            monthData = monthData,
                            yearData = yearData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        HeatMapCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        TrendChartCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        DonutChartCard(
                            sessions = sessions,
                            currentTime = currentTime,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    selectedOverview?.let { overview ->
        PeriodOverviewDialog(
            overview = overview,
            onDismiss = { selectedOverview = null }
        )
    }
}

// --- 数据类与计算逻辑 ---
private enum class PeriodType { WEEK, MONTH, YEAR }

private data class PeriodData(
    val totalDurationSeconds: Int,
    val avgDurationMinutes: Float,
    val chartData: List<Pair<String, Float>>
)

private data class TotalStats(
    val totalCount: Int,
    val totalSeconds: Int,
    val avgMinutes: Float,
    val weekCount: Int,
    val monthCount: Int,
    val yearCount: Int
)

private data class LatestSessionInfo(
    val displayDate: String,
    val time: String,
    val durationText: String,
    val daysAgo: Long,
    val detailText: String,
    val isErrorState: Boolean
)

private data class PeriodOverview(
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

// ── 热力图数据 ──
private data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val monthLabels: List<Pair<Int, String>>,
    val weekCount: Int,
    val maxCount: Int,
    val activeDays: Int,
    val totalDays: Int
)

private data class HeatmapWeek(
    val days: List<HeatmapDay>
)

private data class HeatmapDay(
    val date: LocalDate,
    val count: Int,
    val totalDurationSeconds: Int,
    val isFuture: Boolean
)

private fun calculateHeatmapData(
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
private fun calculateTrendData(
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

private fun calculatePeriodData(
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

private fun isWithinPeriod(
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

private fun calculateLatestInfo(sessions: List<Session>): LatestSessionInfo? {
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
private fun calculatePeriodOverview(
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

// ===================== UI 组件 =====================

@Composable
private fun PeriodChartCard(
    weekData: PeriodData,
    monthData: PeriodData,
    yearData: PeriodData,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val periodDataList = listOf(weekData, monthData, yearData)
    val tabLabels = listOf("本周", "本月", "今年")
    val currentData = periodDataList[selectedTabIndex]

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                tabLabels.forEachIndexed { index, label ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest
                                else Color.Transparent
                            )
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 统计概览 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "总时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration(currentData.totalDurationSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "平均时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (currentData.totalDurationSeconds > 0)
                            "%.1f 分钟".format(currentData.avgDurationMinutes)
                        else "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (currentData.totalDurationSeconds > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 图表 ──
            BarChart(data = currentData.chartData)
        }
    }
}

@Composable
private fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp
) {
    if (data.isEmpty() || data.all { it.second <= 0f }) {
        Box(
            modifier = modifier.height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val rawMax = data.maxOf { it.second }
    val maxValue = if (rawMax <= 0f) 1f else rawMax * 1.2f

    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    val animationProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(chartHeight)
            ) {
                Text(
                    "${maxValue.toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    "${(maxValue / 2).toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    "0",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                val barCount = data.size
                val spacing = when {
                    barCount > 20 -> 2.dp
                    barCount > 10 -> 4.dp
                    else -> 6.dp
                }
                val totalSpacing = spacing * (barCount - 1)
                val availableWidth = maxWidth - totalSpacing
                val barWidth = (availableWidth / barCount).coerceIn(2.dp, 32.dp)
                val showValueLabels = barCount <= 12

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                            .drawBehind {
                                val gridCount = 4
                                for (i in 0..gridCount) {
                                    val y = size.height * i / gridCount
                                    drawLine(
                                        color = outlineVariant.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(
                                spacing,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            data.forEach { (_, value) ->
                                val ratio = (value / maxValue).coerceIn(0f, 1f)
                                val animatedRatio = ratio * animationProgress.value
                                val barHeight = chartHeight * animatedRatio
                                val isMax = value == rawMax && value > 0f
                                val hasValue = value > 0f

                                Column(
                                    modifier = Modifier.width(barWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    if (showValueLabels && hasValue) {
                                        Text(
                                            text = value.toInt().toString(),
                                            style = labelStyle,
                                            fontSize = 9.sp,
                                            color = if (isMax) primary
                                            else onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = if (isMax) FontWeight.SemiBold
                                            else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    } else if (showValueLabels) {
                                        Spacer(Modifier.height(13.dp))
                                    }

                                    val cornerRadius = minOf(6.dp, barWidth / 2)
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .height(barHeight.coerceAtLeast(if (hasValue) 2.dp else 0.dp))
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = cornerRadius,
                                                    topEnd = cornerRadius
                                                )
                                            )
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = if (isMax) {
                                                        listOf(primary, primary.copy(alpha = 0.85f))
                                                    } else {
                                                        listOf(
                                                            primary.copy(alpha = 0.6f),
                                                            primary.copy(alpha = 0.4f)
                                                        )
                                                    }
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            spacing,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        data.forEach { (label, value) ->
                            val isMax = value == rawMax && value > 0f
                            Text(
                                text = label,
                                style = labelStyle,
                                fontSize = if (barCount > 10) 9.sp else 10.sp,
                                color = if (isMax) primary
                                else onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (isMax) FontWeight.SemiBold
                                else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.width(barWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "(。・ω・。)",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "暂无统计数据\n快去完成第一次记录吧！",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TotalStatCard(
    stats: TotalStats,
    sessions: List<Session>,
    onPeriodClick: (PeriodType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = buildTotalStatStatus(sessions)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "总体统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatDuration(stats.totalSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                val avgText =
                    if (stats.totalCount > 0) "%.1f 分钟".format(stats.avgMinutes) else "0 分钟"
                Text(
                    text = "平均每次 $avgText · 共 ${stats.totalCount} 次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (statusText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodStatCard(
                    label = "本周",
                    count = stats.weekCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.WEEK, "本周") }
                )
                PeriodStatCard(
                    label = "本月",
                    count = stats.monthCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.MONTH, "本月") }
                )
                PeriodStatCard(
                    label = "今年",
                    count = stats.yearCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onPeriodClick(PeriodType.YEAR, "今年") }
                )
            }
        }
    }
}

@Composable
private fun PeriodStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LatestSessionCard(
    latestInfo: LatestSessionInfo?,
    modifier: Modifier = Modifier
) {
    val isError = latestInfo?.isErrorState == true

    val gradientBrush = Brush.verticalGradient(
        colors = if (isError) {
            listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
        }
    )

    val contentColor =
        if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val contentColorVariant = contentColor.copy(alpha = 0.8f)
    val overlayColor = contentColor.copy(alpha = 0.12f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxWidth()
        ) {
            if (latestInfo == null) {
                Box(
                    modifier = Modifier.padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有开始记录哦～",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColorVariant
                    )
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = contentColor
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "最近一次 · ${latestInfo.displayDate}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Text(
                                text = "${latestInfo.time} · 坚持了 ${latestInfo.durationText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColorVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(overlayColor)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = latestInfo.detailText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodOverviewDialog(
    overview: PeriodOverview,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.heightIn(max = screenHeight * 0.95f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "${overview.periodLabel}总览",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 总览与周期对比
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "共 ${overview.count} 次 · 总时长 ${formatDuration(overview.totalDurationSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (overview.count > 0 && overview.countComparison.isNotEmpty()) {
                            Text(
                                text = "次数：${overview.countComparison}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (overview.count > 0 && overview.durationComparison.isNotEmpty()) {
                            Text(
                                text = "总时长：${overview.durationComparison}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (overview.count > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "最长记录",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${overview.longestSessionDisplayDate} · ${
                                            formatDuration(overview.longestDurationSeconds)
                                        }",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${overview.longestSessionEndTime} 结束",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.65f
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.3f
                            )
                        )

                        OverviewDetailRow(
                            label = "最多次数的道具",
                            value = "${overview.mostUsedProps.ifEmpty { "无" }} (${overview.mostUsedPropsCount}次)",
                            comparison = overview.propsComparison
                        )
                        OverviewDetailRow(
                            label = "最多次数的心情",
                            value = "${overview.mostCommonMood.ifEmpty { "无" }} (${overview.mostCommonMoodCount}次)",
                            comparison = overview.moodComparison
                        )
                        OverviewDetailRow(
                            label = "最多次数的地点",
                            value = if (overview.mostCommonLocation == "未记录") "未记录"
                            else "${overview.mostCommonLocation} (${overview.mostCommonLocationCount}次)",
                            comparison = overview.locationComparison
                        )
                        OverviewDetailRow(
                            label = "平均评分",
                            value = "%.1f / 5.0".format(overview.avgRating),
                            comparison = overview.avgRatingComparison
                        )
                        OverviewDetailRow(
                            label = "小电影",
                            value = "${overview.movieCount} / ${overview.count} 次",
                            comparison = overview.movieComparison
                        )
                        OverviewDetailRow(
                            label = "高潮",
                            value = "${overview.climaxCount} / ${overview.count} 次",
                            comparison = overview.climaxComparison,
                            showDivider = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun OverviewDetailRow(
    label: String,
    value: String,
    comparison: String = "",
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (comparison.isNotEmpty()) {
                    Text(
                        text = comparison,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}

// ════════════════════════════════════════
// 热力图 HeatMapCard
// ════════════════════════════════════════

@Composable
private fun HeatMapCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val heatmapData by remember(sessions, currentTime) {
        derivedStateOf { calculateHeatmapData(sessions, currentTime) }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "活动热力图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "最近 ${heatmapData.weekCount} 周",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            HeatMapGrid(data = heatmapData)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "少",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                val primary = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
                listOf(
                    surfaceColor,
                    primary.copy(alpha = 0.25f),
                    primary.copy(alpha = 0.5f),
                    primary.copy(alpha = 0.75f),
                    primary
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(11.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "多",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "活跃 ${heatmapData.activeDays} / ${heatmapData.totalDays} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (heatmapData.maxCount > 0) {
                    Text(
                        "单日最高 ${heatmapData.maxCount} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatMapGrid(
    data: HeatmapData,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val futureColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val levels = listOf(
        surfaceColor,
        primary.copy(alpha = 0.25f),
        primary.copy(alpha = 0.5f),
        primary.copy(alpha = 0.75f),
        primary
    )

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val cellSize = 13.dp
    val cellGap = 3.dp
    val labelWidth = 22.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = labelWidth)) {
            data.monthLabels.forEach { (weekIndex, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(
                        start = (cellSize + cellGap) * weekIndex
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                listOf("一", "", "三", "", "五", "", "日").forEach { label ->
                    Box(modifier = Modifier.size(cellSize)) {
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxSize(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                data.weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        week.days.forEach { day ->
                            val level = when {
                                day.isFuture -> 5
                                day.count == 0 -> 0
                                data.maxCount == 0 -> 0
                                else -> {
                                    val ratio = day.count.toFloat() / data.maxCount
                                    when {
                                        ratio <= 0.25f -> 1
                                        ratio <= 0.5f -> 2
                                        ratio <= 0.75f -> 3
                                        else -> 4
                                    }
                                }
                            }

                            val cellColor = when (level) {
                                5 -> futureColor
                                0 -> levels[0]
                                else -> levels[level]
                            }

                            val alpha = when (level) {
                                0, 5 -> 1f
                                else -> animatedProgress.value
                            }

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════
// 趋势图 TrendChartCard
// ════════════════════════════════════════

@Composable
private fun TrendChartCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val trendData by remember(sessions, currentTime) {
        derivedStateOf { calculateTrendData(sessions, currentTime) }
    }

    val totalMinutes = trendData.sumOf { it.second.toDouble() }.toFloat()
    val avgMinutes = if (trendData.isNotEmpty()) totalMinutes / trendData.size else 0f
    val rawMax = trendData.maxOfOrNull { it.second } ?: 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "趋势分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "最近 12 周时长变化",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "周均时长",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "%.1f 分钟".format(avgMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "12周总计",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration((totalMinutes * 60).toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            LineChart(data = trendData, rawMax = rawMax)
        }
    }
}

@Composable
private fun LineChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    rawMax: Float = 0f
) {
    if (data.isEmpty() || data.all { it.second <= 0f }) {
        Box(
            modifier = modifier
                .height(chartHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = (rawMax * 1.2f).coerceAtLeast(1f)
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "${maxValue.toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    "${(maxValue / 2).toInt()}",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    "0",
                    style = labelStyle,
                    color = onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .fillMaxSize()
                    .drawBehind {
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = size.height * i / gridCount
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.2f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stepX = if (data.size > 1) w / (data.size - 1) else w

                    val points = data.mapIndexed { index, (_, value) ->
                        val x = index * stepX
                        val y = h - (value / maxValue) * h * animatedProgress.value
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val midX = (prev.x + curr.x) / 2
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.35f),
                                    primary.copy(alpha = 0.0f)
                                )
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = primary,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val value = data[index].second
                        val isMax = value == rawMax && value > 0f
                        drawCircle(
                            color = if (isMax) primary else primary.copy(alpha = 0.7f),
                            radius = (if (isMax) 5.dp else 3.dp).toPx(),
                            center = point
                        )
                        if (isMax) {
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val maxLabels = 6
            val step = ((data.size + maxLabels - 1) / maxLabels).coerceAtLeast(1)
            data.forEachIndexed { index, (label, _) ->
                if (index % step == 0 || index == data.size - 1) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════
// 圆环图 DonutChartCard
// ════════════════════════════════════════

@Composable
private fun DonutChartCard(
    sessions: List<Session>,
    currentTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("心情", "道具", "地点")

    val distribution by remember(sessions, currentTime, selectedTabIndex) {
        derivedStateOf {
            val filtered = sessions.filter {
                isWithinPeriod(it.timestamp, currentTime, PeriodType.YEAR)
            }
            val map = when (selectedTabIndex) {
                0 -> filtered.groupingBy { it.mood.ifEmpty { "未记录" } }.eachCount()
                1 -> filtered.groupingBy { it.props.ifEmpty { "无" } }.eachCount()
                2 -> filtered.groupingBy { it.location.ifEmpty { "未记录" } }.eachCount()
                else -> emptyMap()
            }
            val sorted = map.entries.sortedByDescending { it.value }
            val top = sorted.take(6).map { it.key to it.value }
            if (sorted.size > 6) {
                val othersCount = sorted.drop(6).sumOf { it.value }
                top + ("其他" to othersCount)
            } else {
                top
            }
        }
    }

    val total = distribution.sumOf { it.second }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DonutLarge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "分布统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest
                                else Color.Transparent
                            )
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold
                            else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        data = distribution,
                        total = total,
                        modifier = Modifier.size(140.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        distribution.forEachIndexed { index, (label, count) ->
                            val color = donutColors[index % donutColors.size]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "%.0f%%".format(count.toFloat() / total * 100),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<Pair<String, Int>>,
    total: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val bgRingColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val colors = remember(primary, secondary, tertiary, error) {
        listOf(
            primary,
            secondary,
            tertiary,
            error,
            Color(0xFF8E24AA),
            Color(0xFF00897B),
            Color(0xFFEF6C00)
        )
    }

    val animatedProgress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width, size.height)
            val strokePx = 18.dp.toPx()
            val radius = (diameter - strokePx) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = bgRingColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            var startAngle = -90f
            data.forEachIndexed { index, (_, count) ->
                val sweep = (count.toFloat() / total) * 360f * animatedProgress.value
                if (sweep > 0.5f) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "总计",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
        }
    }
}

// 圆环图颜色列表（在 composable 外定义避免重组）
private val donutColors = listOf(
    Color(0xFF6750A4),
    Color(0xFF625B71),
    Color(0xFF7D5260),
    Color(0xFFB3261E),
    Color(0xFF8E24AA),
    Color(0xFF00897B),
    Color(0xFFEF6C00)
)

// --- 工具函数 ---

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}小时${minutes}分"
    } else {
        "${minutes}分钟"
    }
}

private fun buildTotalStatStatus(sessions: List<Session>): String {
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

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}