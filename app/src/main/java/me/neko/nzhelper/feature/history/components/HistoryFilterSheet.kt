package me.neko.nzhelper.feature.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheet(
    activeFilters: Set<HistoryQuickFilter>,
    onConfirm: (Set<HistoryQuickFilter>) -> Unit,
    onDismiss: () -> Unit
) {
    var selection by remember(activeFilters) { mutableStateOf(activeFilters) }

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "筛选记录",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                HistoryQuickFilter.entries.forEach { filter ->
                    val isSelected = filter in selection
                    item {
                        SettingsItem(
                            icon = filterIcon(filter),
                            title = filter.label,
                            subtitle = filter.description,
                            selected = isSelected,
                            onClick = {
                                selection = when {
                                    filter == HistoryQuickFilter.ALL ->
                                        setOf(HistoryQuickFilter.ALL)

                                    isSelected ->
                                        (selection - filter)
                                            .ifEmpty { setOf(HistoryQuickFilter.ALL) }

                                    else ->
                                        (selection - HistoryQuickFilter.ALL) + filter
                                }
                            },
                            trailingContent = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onConfirm(selection)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("确定")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun filterIcon(filter: HistoryQuickFilter): ImageVector =
    when (filter) {
        HistoryQuickFilter.ALL -> Icons.Outlined.SelectAll
        HistoryQuickFilter.CLIMAX -> Icons.Outlined.Favorite
        HistoryQuickFilter.NO_CLIMAX -> Icons.Outlined.FavoriteBorder
        HistoryQuickFilter.MODE_SOLO_MALE -> Icons.Outlined.Male
        HistoryQuickFilter.MODE_SOLO_FEMALE -> Icons.Outlined.Female
        HistoryQuickFilter.MODE_PAIR -> Icons.Outlined.Group
    }
