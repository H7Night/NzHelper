package me.neko.nzhelper.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.neko.nzhelper.data.SessionFormState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 详情填写弹窗
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsDialog(
    show: Boolean,
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "本次详情",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // 地点
                InputSection(title = "地点") {
                    OutlinedTextField(
                        value = formState.location,
                        onValueChange = { onFormStateChange(formState.copy(location = it)) },
                        placeholder = { Text("例如：卧室") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // 开关组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedFilterChip(
                        selected = formState.watchedMovie,
                        onClick = { onFormStateChange(formState.copy(watchedMovie = !formState.watchedMovie)) },
                        label = { Text("小电影") },
                        leadingIcon = if (formState.watchedMovie) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    ElevatedFilterChip(
                        selected = formState.climax,
                        onClick = { onFormStateChange(formState.copy(climax = !formState.climax)) },
                        label = { Text("高潮") },
                        leadingIcon = if (formState.climax) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 评分
                RatingSection(
                    rating = formState.rating,
                    onRatingChange = { onFormStateChange(formState.copy(rating = it)) }
                )

                // 道具选择
                SelectionSection(
                    title = "道具",
                    items = listOf("手", "斐济杯", "小胶妻", "其他"),
                    selected = formState.props,
                    onSelected = { onFormStateChange(formState.copy(props = it)) }
                )

                // 心情选择
                SelectionSection(
                    title = "心情",
                    items = listOf("平静", "愉悦", "兴奋", "疲惫"),
                    selected = formState.mood,
                    onSelected = { onFormStateChange(formState.copy(mood = it)) }
                )

                // 备注
                InputSection(title = "备注") {
                    OutlinedTextField(
                        value = formState.remark,
                        onValueChange = { onFormStateChange(formState.copy(remark = it)) },
                        placeholder = { Text("有什么想说的...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )
                }

                // 按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("保存记录")
                    }
                }
            }
        }
    }
}

@Composable
private fun InputSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun SelectionSection(
    title: String,
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                AssistChip(
                    onClick = { onSelected(item) },
                    label = { Text(item) },
                    border = if (selected == item) {
                        null
                    } else {
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected == item)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    leadingIcon = if (selected == item) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun RatingSection(
    rating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "评分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%.1f".format(rating),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = rating,
            onValueChange = {
                val rounded = (it * 10).roundToInt() / 10f
                onRatingChange(rounded.coerceIn(0f, 5f))
            },
            valueRange = 0f..5f,
            steps = 49
        )
    }
}

// 通用工具函数
fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format(Locale.getDefault(), "%02d:", hours))
        append(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))
    }
}