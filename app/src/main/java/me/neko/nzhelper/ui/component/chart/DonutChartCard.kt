package me.neko.nzhelper.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.util.isWithinPeriod
import java.time.LocalDateTime

/**
 * 从当前颜色方案派生的圆环图调色板，避免硬编码颜色字面量，
 * 保证深色/浅色模式与动态取色下均与主题一致。
 */
@Composable
private fun rememberDonutColors(): List<Color> {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        listOf(
            cs.primary,
            cs.secondary,
            cs.tertiary,
            cs.error,
            cs.primary.copy(alpha = 0.6f),
            cs.tertiary.copy(alpha = 0.6f),
            cs.secondary.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonutChartCard(
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
    val donutColors = rememberDonutColors()

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
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
                        .clip(MaterialTheme.shapes.medium)
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

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tabs.size
                        ),
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        }
                    )
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
                        colors = donutColors,
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
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val bgRingColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

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