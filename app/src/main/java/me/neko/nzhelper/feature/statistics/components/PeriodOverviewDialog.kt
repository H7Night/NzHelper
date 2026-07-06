package me.neko.nzhelper.feature.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.neko.nzhelper.feature.statistics.model.PeriodOverview
import me.neko.nzhelper.feature.statistics.util.formatDuration

@Composable
fun PeriodOverviewDialog(
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
            shape = MaterialTheme.shapes.extraLarge,
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
                            shape = MaterialTheme.shapes.large,
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
                                        .clip(MaterialTheme.shapes.medium)
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
                    shape = MaterialTheme.shapes.large
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