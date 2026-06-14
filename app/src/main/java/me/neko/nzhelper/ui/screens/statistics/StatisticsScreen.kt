package me.neko.nzhelper.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen() {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    val currentTime = LocalDateTime.now()

    // 使用统一的计算函数获取各周期数据，避免重复逻辑
    val weekData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.WEEK) }
    }
    val monthData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.MONTH) }
    }
    val yearData by remember(sessions, currentTime) {
        derivedStateOf { calculatePeriodData(sessions, currentTime, PeriodType.YEAR) }
    }

    // 总体统计数据
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
                        isWithinPeriod(
                            it.timestamp,
                            currentTime,
                            PeriodType.WEEK
                        )
                    },
                    monthCount = sessions.count {
                        isWithinPeriod(
                            it.timestamp,
                            currentTime,
                            PeriodType.MONTH
                        )
                    },
                    yearCount = sessions.count {
                        isWithinPeriod(
                            it.timestamp,
                            currentTime,
                            PeriodType.YEAR
                        )
                    }
                )
            }
        }
    }

    val latestInfo by remember(sessions) {
        derivedStateOf { calculateLatestInfo(sessions) }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计") },
                scrollBehavior = scrollBehavior
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 顶部卡片区域
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LatestSessionCard(
                                latestInfo = latestInfo,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TotalStatCard(
                                stats = totalStats,
                                sessions = sessions,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        PeriodSection(
                            title = "本周",
                            data = weekData
                        )
                    }

                    item {
                        PeriodSection(
                            title = "本月",
                            data = monthData
                        )
                    }

                    item {
                        PeriodSection(
                            title = "今年",
                            data = yearData
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
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

/**
 * 统一的周期数据计算
 */
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

    // 为了图表好看，只生成有数据的点
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
    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)

    val displayDate = when (daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        else -> lastDate.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
    }

    val time = latest.timestamp.format(DateTimeFormatter.ofPattern("a h:mm", Locale.CHINA))
    val durationText = formatDuration(latest.duration)

    val isErrorState = daysAgo > 1

    val detailText = when (daysAgo) {
        0L -> getRandomComment(0)
        1L -> getRandomComment(1)
        else -> getRandomComment(daysAgo.toInt())
    }

    return LatestSessionInfo(displayDate, time, durationText, daysAgo, detailText, isErrorState)
}

private fun getRandomComment(days: Int): String {
    return when (days) {
        0 -> listOf(
            "适度释放，有益身心",
            "记得多喝水，补充水分",
            "注意休息，不要过度劳累",
            "保持良好的生活习惯"
        ).random()

        1 -> listOf(
            "昨日适度，保持规律",
            "劳逸结合最重要",
            "注意频率，呵护身体"
        ).random()

        2 -> listOf(
            "坚持了两天，自律真棒",
            "养精蓄锐，状态回升",
            "身体正在自我修复中"
        ).random()

        else -> listOf(
            "坚持了 $days 天，非常有毅力",
            "身体健康，精力充沛",
            "继续保持健康的生活方式",
            "身体状态正在恢复"
        ).random()
    }
}


// --- UI 组件 ---
@Composable
private fun PeriodSection(
    title: String,
    data: PeriodData
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 数据概览
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatDuration(data.totalDurationSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "总时长",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (data.totalDurationSeconds > 0) "%.1f分".format(data.avgDurationMinutes) else "0分",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (data.totalDurationSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "平均每次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 图表
        BarChart(data = data.chartData)

        HorizontalDivider(
            modifier = Modifier.padding(top = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "暂无统计数据\n快去完成第一次记录吧！",
                style = MaterialTheme.typography.bodyLarge,
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
    modifier: Modifier = Modifier
) {
    val statusText = buildTotalStatStatus(sessions)

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "总体统计",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatDuration(stats.totalSeconds),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avgText =
                    if (stats.totalCount > 0) "%.1f 分钟".format(stats.avgMinutes) else "0 分钟"
                Text(
                    text = "平均每次 $avgText · 共 ${stats.totalCount} 次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip("本周", stats.weekCount)
                StatChip("本月", stats.monthCount)
                StatChip("今年", stats.yearCount)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int) {
    AssistChip(
        onClick = {},
        label = { Text("$label $value", style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
private fun LatestSessionCard(
    latestInfo: LatestSessionInfo?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (latestInfo?.isErrorState == true)
                MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        if (latestInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有开始记录哦～",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSystemInDarkTheme())
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "最近一次",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = latestInfo.displayDate,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${latestInfo.time} · 坚持了 ${latestInfo.durationText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Text(
                    text = latestInfo.detailText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (latestInfo.isErrorState)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// --- 通用图表组件 ---
@Composable
private fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 240.dp,
    minBarWidth: Dp = 16.dp,
    maxBarWidth: Dp = 54.dp,
    spacing: Dp = 16.dp
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = data.maxOf { it.second }.coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + 60.dp) // 给 X 轴留空间
    ) {

        YAxis(
            maxValue = maxValue,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
        )

        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 动态计算柱子宽度
            val totalSpacing = spacing * (data.size - 1)
            val availableWidth = maxWidth - totalSpacing
            val idealBarWidth = availableWidth / data.size
            val barWidth = idealBarWidth.coerceIn(minBarWidth, maxBarWidth)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(data) { (date, value) ->
                    BarItem(
                        value = value,
                        maxValue = maxValue,
                        date = date,
                        barWidth = barWidth,
                        chartHeight = chartHeight,
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxis(
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "${maxValue.toInt()} 分钟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${(maxValue / 2).toInt()} 分钟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            "0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun BarItem(
    value: Float,
    maxValue: Float,
    date: String,
    barWidth: Dp,
    chartHeight: Dp,
    color: Color
) {
    val ratio = value / maxValue
    val barHeight = chartHeight * ratio

    Column(
        modifier = Modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 绘图区域：Box 占位整个高度，柱子在底部对齐
        Box(
            modifier = Modifier
                .height(chartHeight)
                .fillMaxWidth()
        ) {

            // 柱体
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(barHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(color)
            )

            // 数值显示逻辑：柱子太短时显示在上方，够长时显示在内部
            val showInside = barHeight > 48.dp

            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (showInside) Color.White else Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(
                        y = if (showInside)
                            -barHeight / 2 // 居中于柱体内部
                        else
                            -barHeight - 8.dp // 悬浮于柱体上方
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        // 日期（X 轴）
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
    // 数据少于2条，无法计算间隔
    if (sessions.size < 2) return "刚开始记录，保持适度"

    val firstDate = sessions.first().timestamp.toLocalDate()
    val lastDate = sessions.last().timestamp.toLocalDate()
    // 计算首尾记录的时间跨度（天），至少为1天
    val daysSpan = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1)

    // 计算平均每天的频率 (次/天)
    val frequency = sessions.size.toDouble() / daysSpan

    return when {
        // 每天一次甚至更多 -> 过度
        frequency >= 1.0 -> "平均每天一次以上，频率偏高，建议适度节制"

        // 两三天一次 -> 频繁
        frequency >= 0.3 -> "平均两三天一次，较为频繁，注意休息"

        // 一周一次左右 -> 适度
        frequency >= 0.14 -> "平均一周左右一次，频率适中，身心健康"

        // 更少 -> 节制
        else -> "频率较低，注意保持良好心态"
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    StatisticsScreen()
}