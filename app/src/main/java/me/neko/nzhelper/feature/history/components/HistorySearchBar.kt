package me.neko.nzhelper.feature.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.sessionMode

enum class HistoryQuickFilter(val label: String, val description: String) {
    ALL("全部", "显示全部记录"),
    CLIMAX("高潮", "有高潮的记录"),
    NO_CLIMAX("未高潮", "没有高潮的记录"),
    MODE_SOLO_MALE("男", "男性单人记录"),
    MODE_SOLO_FEMALE("女", "女性单人记录"),
    MODE_PAIR("双人", "双人记录")
}

fun Session.matches(filter: HistoryQuickFilter): Boolean = when (filter) {
    HistoryQuickFilter.ALL -> true
    HistoryQuickFilter.CLIMAX -> climaxCount > 0
    HistoryQuickFilter.NO_CLIMAX -> climaxCount == 0
    HistoryQuickFilter.MODE_SOLO_MALE -> sessionMode() == SessionMode.SOLO_MALE
    HistoryQuickFilter.MODE_SOLO_FEMALE -> sessionMode() == SessionMode.SOLO_FEMALE
    HistoryQuickFilter.MODE_PAIR -> sessionMode() == SessionMode.PAIR
}

@Composable
fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilters: Set<HistoryQuickFilter>,
    resultCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceBright),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索 备注 / 标签 / 日期 / 时长…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            onQueryChange("")
                            keyboard?.hide()
                        }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "清除搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFiltered =
                query.isNotEmpty() || activeFilters.any { it != HistoryQuickFilter.ALL }
            val filterLabel = if (isFiltered) {
                activeFilters.joinToString("、") { it.label }
            } else {
                "全部"
            }
            Text(
                text = "当前筛选：$filterLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            val countText = if (!isFiltered) {
                "共 $totalCount 条"
            } else {
                "$resultCount / $totalCount"
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
