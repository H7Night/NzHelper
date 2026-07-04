package me.neko.nzhelper.feature.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.feature.statistics.model.PeriodType
import me.neko.nzhelper.feature.statistics.model.TotalStats
import me.neko.nzhelper.feature.statistics.util.buildTotalStatStatus
import me.neko.nzhelper.feature.statistics.util.formatDuration

@Composable
fun TotalStatCard(
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
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "总体统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "累计活动概览",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatDuration(stats.totalSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
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
                Spacer(Modifier.height(16.dp))
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

            Spacer(Modifier.height(20.dp))

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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}